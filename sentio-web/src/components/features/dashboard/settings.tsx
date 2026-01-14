import { Header } from '@/components/layout/dashboard/header'
import { Main } from '@/components/layout/dashboard/main'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { Settings, User, Palette, LogOut } from 'lucide-react'
import { useAuth } from '@/context/auth-context'
import { useTheme } from '@/context/theme-provider'
import { useNavigate } from 'react-router-dom'

export default function SettingsPage() {
    const { user, logout } = useAuth()
    const { theme, setTheme } = useTheme()
    const navigate = useNavigate()

    const handleLogout = async () => {
        try {
            await logout()
            navigate('/login')
        } catch (err) {
            console.error(err)
        }
    }

    return (
        <>
            <Header>
                <div className="flex items-center gap-2">
                    <Settings className="h-5 w-5" />
                    <h1 className="text-lg font-semibold">Settings</h1>
                </div>
            </Header>
            <Main>
                {/* Profile Section */}
                <Card className="mb-6">
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <User className="h-5 w-5" />
                            <CardTitle>Profile</CardTitle>
                        </div>
                        <CardDescription>Your account information from Keycloak</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex items-center gap-4">
                            <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center">
                                <User className="h-8 w-8 text-primary" />
                            </div>
                            <div>
                                <p className="text-lg font-semibold">{user?.username || 'User'}</p>
                                <p className="text-sm text-muted-foreground">{user?.email || 'No email'}</p>
                            </div>
                        </div>
                        <Separator />
                        <div className="grid gap-2">
                            <div className="flex justify-between items-center">
                                <span className="text-muted-foreground">Username</span>
                                <span className="font-medium">{user?.username || 'N/A'}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-muted-foreground">Email</span>
                                <span className="font-medium">{user?.email || 'N/A'}</span>
                            </div>
                            <div className="flex justify-between items-center">
                                <span className="text-muted-foreground">Roles</span>
                                <div className="flex gap-1">
                                    {user?.roles?.map((role) => (
                                        <span key={role} className="px-2 py-1 rounded bg-primary/10 text-primary text-xs">
                                            {role}
                                        </span>
                                    )) || <span className="text-muted-foreground text-sm">No roles</span>}
                                </div>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Appearance Section */}
                <Card className="mb-6">
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <Palette className="h-5 w-5" />
                            <CardTitle>Appearance</CardTitle>
                        </div>
                        <CardDescription>Customize the look and feel</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="font-medium">Theme</p>
                                <p className="text-sm text-muted-foreground">Choose your preferred color scheme</p>
                            </div>
                            <div className="flex gap-2">
                                <Button
                                    variant={theme === 'light' ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={() => setTheme('light')}
                                >
                                    Light
                                </Button>
                                <Button
                                    variant={theme === 'dark' ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={() => setTheme('dark')}
                                >
                                    Dark
                                </Button>
                                <Button
                                    variant={theme === 'system' ? 'default' : 'outline'}
                                    size="sm"
                                    onClick={() => setTheme('system')}
                                >
                                    System
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Logout */}
                <Card>
                    <CardContent className="pt-6">
                        <Button variant="destructive" onClick={handleLogout} className="w-full">
                            <LogOut className="h-4 w-4 mr-2" />
                            Sign Out
                        </Button>
                    </CardContent>
                </Card>
            </Main>
        </>
    )
}
