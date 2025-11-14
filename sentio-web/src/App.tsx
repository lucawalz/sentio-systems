import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from './components/shared/theme-context';
import { Layout } from './components/layout/Layout.tsx';
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import LogIn from './pages/LogIn.tsx';
import './index.css';
import React from 'react'

function App() {
    return (
        <ThemeProvider>
            <Router>
                <Layout>
                    <Routes>
                        <Route path="/" element={<Landing />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/login" element={<LogIn />} />
                    </Routes>
                </Layout>
            </Router>
        </ThemeProvider>
    );
}

export default App;