"""Locust load test for Instakill.

Start the backend first (./gradlew bootRun), then from the backend directory:

    locust -f locustfile.py --host=http://localhost:8080

Web UI: http://localhost:8089

Each simulated user logs in once and then repeatedly reads the feed and toggles
likes. Every user logs in as the seeded user "alice" (password "password").
"""

import random

from locust import HttpUser, between, task

LOGIN_PAYLOAD = {"usernameOrEmail": "alice", "password": "password"}


class InstakillUser(HttpUser):
    wait_time = between(1, 2)

    def on_start(self):
        self.post_ids = []
        with self.client.post(
            "/api/auth/login",
            json=LOGIN_PAYLOAD,
            name="POST /api/auth/login",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"login failed with status {response.status_code}")

    def _fetch_feed(self, name, params=None):
        with self.client.get("/api/feed", params=params, name=name, catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"feed failed with status {response.status_code}")
                return
            self.post_ids = [post["id"] for post in response.json()]

    @task(3)
    def feed_for_liking(self):
        self._fetch_feed("GET /api/feed [for liking]")

    @task(2)
    def feed_trending(self):
        self._fetch_feed("GET /api/feed [sort=trending]", params={"sort": "trending"})

    @task(2)
    def feed_newest(self):
        self._fetch_feed("GET /api/feed [sort=new]", params={"sort": "new"})

    @task(3)
    def toggle_like(self):
        if not self.post_ids:
            self._fetch_feed("GET /api/feed [for liking]")
            if not self.post_ids:
                return
        post_id = random.choice(self.post_ids)
        with self.client.post(
            f"/api/posts/{post_id}/likes/toggle",
            name="POST /api/posts/:id/likes/toggle",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(f"toggle failed with status {response.status_code}")
