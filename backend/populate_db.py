#!/usr/bin/env python3
"""Fill the Instakill PostgreSQL database with configurable test data.

Deletes all comments, likes, posts and users (except alice and bob), adds the
user charlie (password: "password") and generates random users with posts,
comments and likes that match the PostgreSQL schema created by Hibernate
(run the backend with the "postgres" profile once before using this script).

Requirements: pip install faker bcrypt "psycopg[binary]"

Examples:
    python populate_db.py
    python populate_db.py --users 500 --max-posts 5 --seed 42
"""

import argparse
import random
import time
import uuid
from datetime import datetime, time as dtime, timedelta, timezone

import bcrypt
import psycopg
from faker import Faker

DEFAULT_DSN = "postgresql://instakill:instakill_password@localhost:5432/instakill"

SEED_USERNAMES = ("alice", "bob")
CHARLIE = ("charlie", "charlie@example.com", "password")
ALICE_BOB_PASSWORD = "password"
PHOTO_EXTENSIONS = ("jpg", "png")
VIDEO_EXTENSIONS = ("mp4", "mov")
BCRYPT_ROUNDS = 10  # Spring's BCryptPasswordEncoder default strength


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dsn", default=DEFAULT_DSN,
                        help=f"PostgreSQL connection string (default: {DEFAULT_DSN})")
    parser.add_argument("--users", type=int, default=10_000,
                        help="total number of users including alice, bob and charlie (default: 10000)")
    parser.add_argument("--min-posts", type=int, default=10, help="minimum posts per user (default: 10)")
    parser.add_argument("--max-posts", type=int, default=20, help="maximum posts per user (default: 20)")
    parser.add_argument("--max-comments", type=int, default=10, help="maximum comments per post (default: 10)")
    parser.add_argument("--max-likes", type=int, default=20, help="maximum likes per post (default: 20)")
    parser.add_argument("--max-words", type=int, default=10, help="maximum words per post (default: 10)")
    parser.add_argument("--batch-size", type=int, default=500,
                        help="number of users written per committed batch (default: 500)")
    parser.add_argument("--seed", type=int, default=None, help="random seed for reproducible data")
    args = parser.parse_args()

    if args.users < len(SEED_USERNAMES) + 1:
        parser.error("--users must be at least 3 (alice, bob and charlie)")
    if not 0 <= args.min_posts <= args.max_posts:
        parser.error("--min-posts must be >= 0 and <= --max-posts")
    if min(args.max_comments, args.max_likes, args.max_words) < 0:
        parser.error("--max-comments, --max-likes and --max-words must not be negative")
    if args.max_words < 1:
        parser.error("--max-words must be at least 1")
    if args.batch_size < 1:
        parser.error("--batch-size must be at least 1")
    return args


def to_timestamp(dt):
    """PostgreSQL stores Instant values as timestamp with time zone (UTC)."""
    return dt.astimezone(timezone.utc)


def unique_username(fake, taken):
    while True:
        name = fake.user_name()[:32]
        if len(name) >= 3 and name not in taken:
            taken.add(name)
            return name


def reset_tables(conn, now):
    """Delete comments, likes, posts and users (except alice and bob) and return the seed user ids."""
    with conn.transaction():
        conn.execute("DELETE FROM comments")
        conn.execute("DELETE FROM likes")
        conn.execute("DELETE FROM posts")
        conn.execute("DELETE FROM users WHERE username NOT IN (%s, %s)", SEED_USERNAMES)

        seed_ids = {}
        for username in SEED_USERNAMES:
            row = conn.execute("SELECT id FROM users WHERE username = %s", (username,)).fetchone()
            if row is None:
                # alice/bob were removed from the database: recreate them with the documented password
                row_id = str(uuid.uuid4())
                conn.execute(
                    "INSERT INTO users (id, created_at, email, password_hash, username) VALUES (%s, %s, %s, %s, %s)",
                    (row_id, to_timestamp(now), f"{username}@example.com", hash_password(ALICE_BOB_PASSWORD), username),
                )
            else:
                row_id = row[0]
            seed_ids[username] = row_id

        charlie_id = str(uuid.uuid4())
        username, email, password = CHARLIE
        conn.execute(
            "INSERT INTO users (id, created_at, email, password_hash, username) VALUES (%s, %s, %s, %s, %s)",
            (charlie_id, to_timestamp(now), email, hash_password(password), username),
        )
        seed_ids[username] = charlie_id
    return seed_ids


def hash_password(password):
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt(rounds=BCRYPT_ROUNDS)).decode("utf-8")


def build_generated_users(fake, count, now, shared_hash):
    """Return (user_rows, user_ids). All generated users share one bcrypt hash of 'password' for speed."""
    taken = {"alice", "bob", CHARLIE[0]}
    earliest = now - timedelta(days=365)
    week_start = now - timedelta(days=7)
    rows = []
    for _ in range(count):
        username = unique_username(fake, taken)
        created = fake.date_time_between(earliest, week_start)
        rows.append((str(uuid.uuid4()), to_timestamp(created), f"{username}@example.com", shared_hash, username))
    return rows, [row[0] for row in rows]


def random_post_time(fake, day, now):
    day_start = datetime.combine(day, dtime.min)
    upper = min(day_start + timedelta(days=1), now)
    return fake.date_time_between(day_start, upper)


def main():
    args = parse_args()
    if args.seed is not None:
        random.seed(args.seed)
        Faker.seed(args.seed)
    fake = Faker()

    now = datetime.now()
    week_start = now - timedelta(days=7)
    started = time.time()

    conn = psycopg.connect(args.dsn, autocommit=True)
    try:
        print("Resetting tables in the PostgreSQL database ...")
        seed_ids = reset_tables(conn, now)

        generated_count = args.users - len(seed_ids)
        shared_hash = hash_password(ALICE_BOB_PASSWORD)
        generated_rows, generated_ids = build_generated_users(fake, generated_count, now, shared_hash)

        all_user_ids = list(seed_ids.values()) + generated_ids
        print(f"Generating {generated_count} users plus alice, bob and charlie ({len(all_user_ids)} total) ...")

        # Users are written in committed batches so partial progress is kept on failure.
        for start in range(0, len(generated_rows), args.batch_size):
            batch = generated_rows[start:start + args.batch_size]
            with conn.transaction(), conn.cursor() as cur:
                cur.executemany(
                    "INSERT INTO users (id, created_at, email, password_hash, username) VALUES (%s, %s, %s, %s, %s)",
                    batch,
                )

        # Posts, comments and likes are generated per author batch and committed together.
        author_ids = list(seed_ids.values()) + generated_ids
        for start in range(0, len(author_ids), args.batch_size):
            post_rows, comment_rows, like_rows = [], [], []
            for author_id in author_ids[start:start + args.batch_size]:
                for _ in range(random.randint(args.min_posts, args.max_posts)):
                    post_id = str(uuid.uuid4())
                    day = fake.date_between_dates(date_start=week_start.date(), date_end=now.date())
                    created = random_post_time(fake, day, now)
                    content = " ".join(fake.words(nb=random.randint(1, args.max_words)))
                    media_kind = random.choice([PHOTO_EXTENSIONS, VIDEO_EXTENSIONS])
                    ext = random.choice(media_kind)
                    image_url = f"/media/{uuid.uuid4()}.{ext}"

                    n_likes = random.randint(0, min(args.max_likes, len(all_user_ids)))
                    liked_by = random.sample(all_user_ids, n_likes)
                    n_comments = random.randint(0, args.max_comments)

                    latest = created
                    for liker in liked_by:
                        t = fake.date_time_between(created, now)
                        latest = max(latest, t)
                        like_rows.append((str(uuid.uuid4()), to_timestamp(t), post_id, liker))
                    for _ in range(n_comments):
                        t = fake.date_time_between(created, now)
                        latest = max(latest, t)
                        comment_rows.append((
                            str(uuid.uuid4()), random.choice(all_user_ids), fake.sentence(nb_words=random.randint(3, 12)),
                            to_timestamp(t), post_id,
                        ))

                    post_rows.append((
                        post_id, author_id, n_comments, content, to_timestamp(created), image_url,
                        n_likes, to_timestamp(latest),
                    ))

            with conn.transaction(), conn.cursor() as cur:
                cur.executemany(
                    "INSERT INTO posts (id, author_id, comment_count, content, created_at, image_url, like_count, updated_at) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
                    post_rows,
                )
                cur.executemany(
                    "INSERT INTO comments (id, author_id, content, created_at, post_id) VALUES (%s, %s, %s, %s, %s)",
                    comment_rows,
                )
                cur.executemany(
                    "INSERT INTO likes (id, created_at, post_id, user_id) VALUES (%s, %s, %s, %s)",
                    like_rows,
                )
            print(f"  committed authors {start + 1}-{min(start + args.batch_size, len(author_ids))}")

        verify(conn)
    finally:
        conn.close()
    print(f"Done in {time.time() - started:.1f}s")


def verify(conn):
    # Joins/GROUP BY instead of correlated subqueries: the likes table has no index on post_id.
    checks = {
        "posts without existing author": (
            "SELECT COUNT(*) FROM posts p LEFT JOIN users u ON u.id = p.author_id WHERE u.id IS NULL"
        ),
        "comments without post": (
            "SELECT COUNT(*) FROM comments c LEFT JOIN posts p ON p.id = c.post_id WHERE p.id IS NULL"
        ),
        "comments without author": (
            "SELECT COUNT(*) FROM comments c LEFT JOIN users u ON u.id = c.author_id WHERE u.id IS NULL"
        ),
        "likes without post": (
            "SELECT COUNT(*) FROM likes l LEFT JOIN posts p ON p.id = l.post_id WHERE p.id IS NULL"
        ),
        "likes without user": (
            "SELECT COUNT(*) FROM likes l LEFT JOIN users u ON u.id = l.user_id WHERE u.id IS NULL"
        ),
        "duplicate likes": (
            "SELECT COUNT(*) FROM (SELECT post_id, user_id FROM likes GROUP BY post_id, user_id HAVING COUNT(*) > 1)"
        ),
        "wrong like_count": (
            "SELECT COUNT(*) FROM posts p LEFT JOIN "
            "(SELECT post_id, COUNT(*) AS n FROM likes GROUP BY post_id) l ON l.post_id = p.id "
            "WHERE p.like_count != COALESCE(l.n, 0)"
        ),
        "wrong comment_count": (
            "SELECT COUNT(*) FROM posts p LEFT JOIN "
            "(SELECT post_id, COUNT(*) AS n FROM comments GROUP BY post_id) c ON c.post_id = p.id "
            "WHERE p.comment_count != COALESCE(c.n, 0)"
        ),
    }
    print("Integrity checks:")
    problems = 0
    for name, sql in checks.items():
        count = conn.execute(sql).fetchone()[0]
        problems += count
        print(f"  {name}: {count}")
    users, posts, comments, likes = (
        conn.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        for table in ("users", "posts", "comments", "likes")
    )
    print(f"Totals: users={users} posts={posts} comments={comments} likes={likes}")
    if problems:
        raise SystemExit(f"Integrity check failed with {problems} problem(s)")


if __name__ == "__main__":
    main()
