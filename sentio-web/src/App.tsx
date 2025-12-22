import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from './components/shared/theme-context';
import { AuthProvider } from './context/auth';
import { AnimalProvider } from './context/AnimalContext';
import { WeatherProvider } from './context/WeatherContext';
import { ForecastProvider } from './context/ForecastContext';
import { AiSummaryProvider } from './context/AiSummaryContext';
import { DeviceProvider } from './context/DeviceContext';
import { WebSocketProvider } from './context/WebSocketContext';
import { Layout } from './components/layout/Layout.tsx';
import { ProtectedRoute } from './components/auth/protected-route.tsx';
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import LogIn from './pages/LogIn.tsx';
import Contact from "./pages/Contact.tsx";
import Privacy from "./pages/Privacy.tsx";
import './index.css';

function App() {
    return (
        <AuthProvider>
            <ThemeProvider>
                <Router>
                    <Layout>
                        <Routes>
                            <Route path="/" element={<Landing />} />
                            <Route element={<ProtectedRoute />}>
                                <Route path="/dashboard" element={
                                    <WebSocketProvider>
                                        <DeviceProvider>
                                            <AnimalProvider>
                                                <WeatherProvider>
                                                    <ForecastProvider>
                                                        <AiSummaryProvider>
                                                            <Dashboard />
                                                        </AiSummaryProvider>
                                                    </ForecastProvider>
                                                </WeatherProvider>
                                            </AnimalProvider>
                                        </DeviceProvider>
                                    </WebSocketProvider>
                                } />
                            </Route>
                            <Route path="/login" element={<LogIn mode="login" />} />
                            <Route path="/create-account" element={<LogIn mode="register" />} />
                            <Route path="/contact" element={<Contact />} />
                            <Route path="/privacy" element={<Privacy />} />
                        </Routes>
                    </Layout>
                </Router>
            </ThemeProvider>
        </AuthProvider>
    );
}

export default App;