import {BrowserRouter, Route, Routes, useLocation} from 'react-router-dom'
import {HeroHeader} from '@/components/layout/Header'
import Footer from '@/components/layout/Footer'
import ScrollToTop from '@/components/common/ScrollToTop'
import {AnimatePresence, motion} from 'framer-motion'
import Home from '@/pages/Home'
import About from '@/pages/About'
import Login from '@/pages/Login'
import SignUp from '@/pages/SignUp'
import ForgotPassword from '@/pages/ForgotPassword'
import Contact from '@/pages/Contact'
import DashboardPage from '@/pages/Dashboard'
import Privacy from '@/pages/Privacy.tsx'
import {AuthProvider} from '@/context/auth-context'
import {DeviceProvider} from '@/context/device-context'
import {ProtectedRoute} from '@/components/common/ProtectedRoute'

const pageVariants = {
  initial: {
    opacity: 0,
    y: 20,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.4,
      ease: "easeOut" as const,
    },
  },
  exit: {
    opacity: 0,
    y: -20,
    transition: {
      duration: 0.3,
      ease: "easeIn" as const,
    },
  },
}

// Layout with header/footer for public pages
function PublicLayout() {
  const location = useLocation()

  return (
    <>
      <HeroHeader />
      <AnimatePresence mode="wait">
        <motion.div
          key={location.pathname}
          initial="initial"
          animate="animate"
          exit="exit"
          variants={pageVariants}
        >
          <Routes location={location}>
            <Route path="/" element={<Home />} />
            <Route path="/about" element={<About />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<SignUp />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/contact" element={<Contact />} />
            <Route path="/privacy" element={<Privacy/>}/>
          </Routes>
        </motion.div>
      </AnimatePresence>
      <Footer />
    </>
  )
}

function AppRoutes() {
  const location = useLocation()
  const isDashboard = location.pathname.startsWith('/dashboard')

  // Dashboard has its own layout (sidebar), no header/footer
  if (isDashboard) {
    return (
      <DeviceProvider>
        <Routes>
          <Route path="/dashboard/*" element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          } />
        </Routes>
      </DeviceProvider>
    )
  }

  // Public pages with header/footer
  return <PublicLayout />
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ScrollToTop />
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
