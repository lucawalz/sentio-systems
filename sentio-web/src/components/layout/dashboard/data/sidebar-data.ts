import {
    LayoutDashboard,
    Settings,
    Cloud,
    Activity,
    Cpu,
} from 'lucide-react'
import { type SidebarData } from '../types'

export const sidebarData: SidebarData = {
    teams: [
        {
            name: 'Sentio Systems',
            plan: 'AI-Powered IoT',
        },
    ],
    navGroups: [
        {
            title: 'General',
            items: [
                {
                    title: 'Home',
                    url: '/dashboard',
                    icon: LayoutDashboard,
                },
                {
                    title: 'Weather',
                    url: '/dashboard/weather',
                    icon: Cloud,
                },
                {
                    title: 'Monitoring',
                    url: '/dashboard/monitoring',
                    icon: Activity,
                },
                {
                    title: 'Devices',
                    url: '/dashboard/devices',
                    icon: Cpu,
                },
            ],
        },
        {
            title: 'Settings',
            items: [
                {
                    title: 'Settings',
                    url: '/dashboard/settings',
                    icon: Settings,
                },
            ],
        },
    ],
}
