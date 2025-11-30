import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from './components/shared/theme-context';
import { Layout } from './components/layout/Layout.tsx';
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import LogIn from './pages/LogIn.tsx';
import Contact from "./pages/Contact.tsx";
import Privacy from "./pages/Privacy.tsx";
import './index.css';

function App() {
    return (
        <ThemeProvider>
            <Router>
                <Layout>
                    <Routes>
                        <Route path="/" element={<Landing />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/login" element={<LogIn mode="login"/>} />
                        <Route path="/create-account" element={<LogIn mode="register" />} />
                        <Route path="/contact" element={<Contact/>}/>
                        <Route path="/privacy" element={<Privacy/>} />
                    </Routes>
                </Layout>
            </Router>
        </ThemeProvider>
    );
}

export default App;