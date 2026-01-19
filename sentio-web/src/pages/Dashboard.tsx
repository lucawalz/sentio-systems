import { Routes, Route } from 'react-router-dom'
import { AuthenticatedLayout } from '@/components/layout/dashboard/authenticated-layout'
import { Dashboard } from '@/components/features/dashboard'
import WeatherPage from '@/components/features/dashboard/weather'
import MonitoringPage from '@/components/features/dashboard/monitoring'
import DevicesPage from '@/components/features/dashboard/devices'
import SettingsPage from '@/components/features/dashboard/settings'

export default function DashboardPage() {
    return (
        <AuthenticatedLayout>
            <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/weather" element={<WeatherPage />} />
                <Route path="/monitoring" element={<MonitoringPage />} />
                <Route path="/devices" element={<DevicesPage />} />
                <Route path="/settings" element={<SettingsPage />} />
            </Routes>
        </AuthenticatedLayout>
    )
}
