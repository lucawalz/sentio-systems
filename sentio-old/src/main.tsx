import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { AuthProvider } from './context/auth'

// NOTE: The app is wrapped with `AuthProvider` so any component can use
// `useAuth()` safely. To add a real login page:
// 1) Create `src/pages/Login.tsx` with your login form.
// 2) In `src/App.tsx` add: <Route path="/login" element={<Login />} />
// 3) Replace `onClick={() => login()}` handlers (in navigation or CTA)
//    with navigation to `/login` or open a login modal.
// This comment shows exactly where the login screen should be linked from.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
