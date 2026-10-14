<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>Instakill</h1>
      <nav v-if="user">
        <span class="user">@{{ user.username }}</span>
        <button @click="logout">Logout</button>
      </nav>
    </header>

    <main class="content">
      <section v-if="!user" class="card">
        <div class="tabs">
          <button :class="{ active: authMode === 'login' }" @click="authMode = 'login'">Login</button>
          <button :class="{ active: authMode === 'register' }" @click="authMode = 'register'">Register</button>
        </div>
        <form @submit.prevent="submitAuth" class="form">
          <input v-model="authForm.usernameOrEmail" v-if="authMode === 'login'" placeholder="Username or email" required>
          <input v-model="authForm.username" v-else placeholder="Username" required minlength="3">
          <input v-model="authForm.email" v-if="authMode === 'register'" type="email" placeholder="Email" required>
          <input v-model="authForm.password" type="password" placeholder="Password" required minlength="6">
          <p v-if="error" class="error">{{ error }}</p>
          <button type="submit">Continue</button>
        </form>
      </section>

      <section v-else class="feed">
        <form @submit.prevent="createNewPost" class="card form composer">
          <textarea v-model="composer.content" rows="3" placeholder="Share something..." required></textarea>
          <input v-model="composer.imageUrl" placeholder="Image URL (optional)">
          <div class="actions">
            <button type="submit">Post</button>
          </div>
        </form>

        <div class="feed-toolbar">
          <h2>Feed</h2>
          <div class="sort">
            <button :class="{ active: sort === 'new' }" @click="changeSort('new')">Newest</button>
            <button :class="{ active: sort === 'trending' }" @click="changeSort('trending')">Trending</button>
          </div>
        </div>

        <article v-for="post in posts" :key="post.id" class="card post">
          <header>
            <div>
              <h3>@{{ post.author.username }}</h3>
              <small>{{ formatDate(post.createdAt) }}</small>
            </div>
            <div class="metrics">
              <span>{{ post.likeCount }} likes</span>
              <span> | </span>
              <span>{{ post.commentCount }} comments</span>
            </div>
          </header>
          <p>{{ post.content }}</p>
          <img v-if="post.imageUrl" :src="post.imageUrl" alt="post image">
          
          <!-- Comments Section -->
          <section class="comments">
            <div v-if="comments[post.id] && comments[post.id].length">
              <div v-for="comment in comments[post.id]" :key="comment.id" class="comment">
                <b>@{{ comment.author.username }}</b> <small>{{ formatDate(comment.createdAt) }}</small>
                <div>{{ comment.content }}</div>
              </div>
            </div>
            <div v-else class="no-comments">
              <em>No comments yet.</em>
            </div>
            <form v-if="user" @submit.prevent="submitComment(post.id)" class="comment-form">
              <input
                v-model="newComment[post.id]"
                :placeholder="'Add a comment...'"
                required
                minlength="1"
              />
              <button type="submit" :disabled="commentLoading[post.id]">Comment</button>
            </form>
            <div v-if="commentError[post.id]" class="error">{{ commentError[post.id] }}</div>
          </section>

          <footer>
            <button @click="togglePostLike(post)">
              {{ post.likedByCurrentUser ? 'Unlike' : 'Like' }}
            </button>
          </footer>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createPost, fetchMe, listFeed, login, register as registerUser, toggleLike, listComments, addComment } from './api'

const user = ref(null)
const posts = ref([])
const sort = ref('new')
const authMode = ref('login')
const error = ref('')
const authForm = reactive({ username: '', email: '', password: '', usernameOrEmail: '' })
const composer = reactive({ content: '', imageUrl: '' })

// Comments state
const comments = reactive({})
const newComment = reactive({})
const commentLoading = reactive({})
const commentError = reactive({})

onMounted(async () => {
  await bootstrap()
})

async function bootstrap() {
  try {
    user.value = await fetchMe()
    await loadFeed()
  } catch (e) {
    user.value = null
  }
}

async function submitAuth() {
  error.value = ''
  try {
    if (authMode.value === 'login') {
      const result = await login({
        usernameOrEmail: authForm.usernameOrEmail,
        password: authForm.password
      })
      user.value = result.user
    } else {
      const result = await registerUser({
        username: authForm.username,
        email: authForm.email,
        password: authForm.password
      })
      user.value = result.user
    }
    await loadFeed()
  } catch (e) {
    error.value = e.response?.data?.detail || 'Authentication failed'
  }
}

async function loadFeed() {
  posts.value = await listFeed({ sort: sort.value })
  // Load comments for each post
  for (const post of posts.value) {
    await loadComments(post.id)
  }
}

async function loadComments(postId) {
  try {
    commentError[postId] = ''
    comments[postId] = await listComments(postId)
  } catch (e) {
    comments[postId] = []
    commentError[postId] = 'Failed to load comments'
  }
}

async function submitComment(postId) {
  if (!newComment[postId] || !newComment[postId].trim()) return
  commentLoading[postId] = true
  commentError[postId] = ''
  try {
    await addComment(postId, { content: newComment[postId] })
    newComment[postId] = ''
    await loadComments(postId)
    // Optionally, update commentCount in post
    const post = posts.value.find(p => p.id === postId)
    if (post) post.commentCount = (comments[postId]?.length || 0)
  } catch (e) {
    commentError[postId] = e.response?.data?.detail || 'Failed to add comment'
  } finally {
    commentLoading[postId] = false
  }
}

async function createNewPost() {
  if (!composer.content.trim()) return
  await createPost({ content: composer.content, imageUrl: composer.imageUrl || null })
  composer.content = ''
  composer.imageUrl = ''
  await loadFeed()
}

async function togglePostLike(post) {
  const result = await toggleLike(post.id)
  post.likedByCurrentUser = result.liked
  post.likeCount = result.likeCount
}

function changeSort(next) {
  if (sort.value === next) return
  sort.value = next
  loadFeed()
}

function formatDate(iso) {
  return new Date(iso).toLocaleString()
}

async function logout() {
  try {
    await fetch('/auth/logout', { method: 'POST', credentials: 'include' })
  } catch (e) {
    // ignore
  }
  document.cookie = 'ACCESS_TOKEN=; Max-Age=0; path=/'
  user.value = null
  posts.value = []
}
</script>

<style scoped>
.app-shell {
  max-width: 960px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.app-header nav {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.app-header button {
  background: #111827;
  color: #fff;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 999px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.card {
  background: #fff;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(17, 24, 39, 0.1);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form input,
.form textarea {
  width: 100%;
  padding: 0.75rem;
  border-radius: 10px;
  border: 1px solid #d1d5db;
}

.form button {
  align-self: flex-start;
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 0.5rem 1.25rem;
  border-radius: 999px;
}

.tabs {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.tabs button {
  background: transparent;
  border: none;
  font-weight: 600;
  color: #6b7280;
}

.tabs button.active {
  color: #111827;
}

.error {
  color: #ef4444;
}

.feed-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sort button {
  margin-left: 0.5rem;
  border: none;
  background: #e5e7eb;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
}

.sort button.active {
  background: #111827;
  color: #fff;
}

.post header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.post img {
  margin-top: 0.75rem;
  width: 100%;
  border-radius: 12px;
}

.post footer {
  margin-top: 1rem;
}

.post footer button {
  background: #111827;
  border: none;
  color: #fff;
  padding: 0.4rem 1.2rem;
  border-radius: 999px;
}
.comments {
  margin-top: 1.2rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}
.comment {
  margin-bottom: 0.7rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f3f4f6;
}
.no-comments {
  color: #6b7280;
  margin-bottom: 0.7rem;
}
.comment-form {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
.comment-form input {
  flex: 1;
  padding: 0.5rem;
  border-radius: 8px;
  border: 1px solid #d1d5db;
}
.comment-form button {
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 0.4rem 1.1rem;
  border-radius: 999px;
}
</style>
