import { useState, useEffect } from 'react'
import { Header } from '@/components/layout/dashboard/header'
import { Main } from '@/components/layout/dashboard/main'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog'
import { Cpu, Plus, Wifi, WifiOff, Clock, RefreshCcw, Signal, HardDrive, Zap } from 'lucide-react'
import { AnimatePresence, motion } from 'framer-motion'
import { devicesApi } from '@/lib/api'
import type { Device } from '@/types/api'
import { cn } from '@/lib/utils'
import { NativeDelete } from '@/components/ui/native-delete-shadcnui'

export default function DevicesPage() {
    const [devices, setDevices] = useState<Device[]>([])
    const [loading, setLoading] = useState(true)
    const [hoveredDevice, setHoveredDevice] = useState<string | null>(null)
    const [registerOpen, setRegisterOpen] = useState(false)
    const [registering, setRegistering] = useState(false)
    const [newDeviceId, setNewDeviceId] = useState('')
    const [newDeviceName, setNewDeviceName] = useState('')

    // Edit state
    const [editDevice, setEditDevice] = useState<Device | null>(null)
    const [editName, setEditName] = useState('')
    const [editOpen, setEditOpen] = useState(false)

    const fetchDevices = async () => {
        try {
            setLoading(true)
            const res = await devicesApi.list()
            const data = res.data
            setDevices(Array.isArray(data) ? data : [])
        } catch (error) {
            console.error('Failed to fetch devices:', error)
            setDevices([])
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchDevices()
    }, [])

    const handleRegister = async () => {
        if (!newDeviceId.trim()) return
        try {
            setRegistering(true)
            await devicesApi.register(newDeviceId.trim(), newDeviceName.trim() || 'My Device')
            setNewDeviceId('')
            setNewDeviceName('')
            setRegisterOpen(false)
            await fetchDevices()
        } catch (error) {
            console.error('Failed to register device:', error)
        } finally {
            setRegistering(false)
        }
    }

    const handleUnregister = async (deviceId: string) => {
        try {
            await devicesApi.unregister(deviceId)
            await fetchDevices()
        } catch (error) {
            console.error('Failed to unregister device:', error)
        }
    }

    const handleEditOpen = (device: Device) => {
        setEditDevice(device)
        setEditName(device.name)
        setEditOpen(true)
    }

    const handleEditSave = async () => {
        if (!editDevice || !editName.trim()) return
        setEditOpen(false)
        setEditDevice(null)
    }

    const isOnline = (device: Device) => {
        if (!device.lastSeen) return false
        // Backend sends LocalDateTime which implies UTC but lacks offset
        // We append 'Z' to ensure browser treats it as UTC
        const lastSeenTime = device.lastSeen.endsWith('Z')
            ? device.lastSeen
            : `${device.lastSeen}Z`
        return new Date(lastSeenTime).getTime() > Date.now() - 5 * 60 * 1000
    }

    const formatLastSeen = (lastSeen: string | null) => {
        if (!lastSeen) return 'Never'
        // Treat as UTC
        const lastSeenTime = lastSeen.endsWith('Z') ? lastSeen : `${lastSeen}Z`
        const date = new Date(lastSeenTime)

        const now = new Date()
        const diffMs = now.getTime() - date.getTime()
        const diffMins = Math.floor(diffMs / 60000)
        const diffHours = Math.floor(diffMins / 60)
        const diffDays = Math.floor(diffHours / 24)

        if (diffMins < 1) return 'Just now'
        if (diffMins < 60) return `${diffMins}m ago`
        if (diffHours < 24) return `${diffHours}h ago`
        return `${diffDays}d ago`
    }

    const getUptime = (device: Device) => {
        if (!device.lastSeen) return 0
        const diffMs = Date.now() - new Date(device.lastSeen).getTime()
        if (diffMs < 5 * 60 * 1000) return 99.9
        if (diffMs < 60 * 60 * 1000) return 95
        if (diffMs < 24 * 60 * 60 * 1000) return 80
        return 50
    }

    return (
        <>
            <Header>
                <div className="flex items-center gap-2">
                    <Cpu className="h-5 w-5" />
                    <h1 className="text-lg font-semibold">Devices</h1>
                </div>
                <div className="ml-auto flex items-center gap-2">
                    <Button variant="outline" size="sm" onClick={fetchDevices} disabled={loading}>
                        <RefreshCcw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
                        Refresh
                    </Button>
                    <Dialog open={registerOpen} onOpenChange={setRegisterOpen}>
                        <DialogTrigger asChild>
                            <Button size="sm">
                                <Plus className="h-4 w-4 mr-2" />
                                Register Device
                            </Button>
                        </DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>Register Device</DialogTitle>
                                <DialogDescription>
                                    Enter the device ID from your IoT device to register it to your account.
                                </DialogDescription>
                            </DialogHeader>
                            <div className="grid gap-4 py-4">
                                <div className="grid gap-2">
                                    <Label htmlFor="deviceId">Device ID</Label>
                                    <Input
                                        id="deviceId"
                                        placeholder="e.g., raspi-sensor-01"
                                        value={newDeviceId}
                                        onChange={(e) => setNewDeviceId(e.target.value)}
                                    />
                                </div>
                                <div className="grid gap-2">
                                    <Label htmlFor="deviceName">Device Name (optional)</Label>
                                    <Input
                                        id="deviceName"
                                        placeholder="e.g., Weather Station"
                                        value={newDeviceName}
                                        onChange={(e) => setNewDeviceName(e.target.value)}
                                    />
                                </div>
                            </div>
                            <DialogFooter>
                                <Button variant="outline" onClick={() => setRegisterOpen(false)}>
                                    Cancel
                                </Button>
                                <Button onClick={handleRegister} disabled={registering || !newDeviceId.trim()}>
                                    {registering ? 'Registering...' : 'Register'}
                                </Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </div>
            </Header>

            <Main>
                {loading ? (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {[1, 2, 3].map((i) => (
                            <Skeleton key={i} className="h-32 rounded-xl" />
                        ))}
                    </div>
                ) : devices.length === 0 ? (
                    <Card className="border-dashed">
                        <CardContent className="flex flex-col items-center justify-center py-12">
                            <Cpu className="h-12 w-12 text-muted-foreground mb-4" />
                            <h3 className="text-lg font-semibold mb-2">No devices registered</h3>
                            <p className="text-sm text-muted-foreground text-center mb-4">
                                Register your IoT devices to start collecting weather data and animal detections.
                            </p>
                            <Button onClick={() => setRegisterOpen(true)}>
                                <Plus className="h-4 w-4 mr-2" />
                                Register Your First Device
                            </Button>
                        </CardContent>
                    </Card>
                ) : (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {devices.map((device) => {
                            const online = isOnline(device)
                            const uptime = getUptime(device)
                            const isHovered = hoveredDevice === device.id

                            return (
                                <motion.div
                                    key={device.id}
                                    className="relative"
                                    onMouseEnter={() => setHoveredDevice(device.id)}
                                    onMouseLeave={() => setHoveredDevice(null)}
                                    layout
                                >
                                    <Card
                                        className={cn(
                                            "overflow-hidden cursor-pointer transition-all duration-200",
                                            isHovered && "shadow-lg ring-1 ring-primary/20"
                                        )}
                                        onClick={() => handleEditOpen(device)}
                                    >
                                        <CardHeader className="pb-3">
                                            <div className="flex items-start justify-between">
                                                <div className="flex items-center gap-3">
                                                    <div className={`p-2 rounded-lg ${online ? 'bg-green-500/10' : 'bg-muted'}`}>
                                                        {online ? (
                                                            <Wifi className="h-5 w-5 text-green-500" />
                                                        ) : (
                                                            <WifiOff className="h-5 w-5 text-muted-foreground" />
                                                        )}
                                                    </div>
                                                    <div>
                                                        <CardTitle className="text-base">{device.name}</CardTitle>
                                                        <CardDescription className="text-xs font-mono">
                                                            {device.id}
                                                        </CardDescription>
                                                    </div>
                                                </div>
                                                <Badge variant={online ? 'default' : 'secondary'}>
                                                    {online ? 'Online' : 'Offline'}
                                                </Badge>
                                            </div>
                                        </CardHeader>
                                        <CardContent>
                                            {/* Default view - always visible */}
                                            <div className="flex items-center justify-between text-sm">
                                                <span className="text-muted-foreground flex items-center gap-1">
                                                    <Clock className="h-3 w-3" />
                                                    Last Seen
                                                </span>
                                                <span>{formatLastSeen(device.lastSeen)}</span>
                                            </div>

                                            {/* Expanded content on hover - smooth animation */}
                                            <AnimatePresence>
                                                {isHovered && (
                                                    <motion.div
                                                        initial={{ opacity: 0, height: 0 }}
                                                        animate={{ opacity: 1, height: 'auto' }}
                                                        exit={{ opacity: 0, height: 0 }}
                                                        transition={{ duration: 0.2 }}
                                                        className="overflow-hidden"
                                                    >
                                                        <div className="pt-3 mt-3 border-t border-border/50 space-y-2">
                                                            <div className="flex items-center justify-between text-sm">
                                                                <span className="text-muted-foreground flex items-center gap-1.5">
                                                                    <Signal className="h-3.5 w-3.5" />
                                                                    Signal
                                                                </span>
                                                                <span className="font-medium">{online ? 'Strong' : 'None'}</span>
                                                            </div>
                                                            <div className="flex items-center justify-between text-sm">
                                                                <span className="text-muted-foreground flex items-center gap-1.5">
                                                                    <Zap className="h-3.5 w-3.5" />
                                                                    Uptime
                                                                </span>
                                                                <span className="font-medium">{uptime.toFixed(1)}%</span>
                                                            </div>
                                                            <div className="flex items-center justify-between text-sm">
                                                                <span className="text-muted-foreground flex items-center gap-1.5">
                                                                    <HardDrive className="h-3.5 w-3.5" />
                                                                    Type
                                                                </span>
                                                                <span className="font-medium">Raspberry Pi</span>
                                                            </div>
                                                            <p className="text-[11px] text-muted-foreground text-center pt-2">
                                                                Click to manage
                                                            </p>
                                                        </div>
                                                    </motion.div>
                                                )}
                                            </AnimatePresence>
                                        </CardContent>
                                    </Card>
                                </motion.div>
                            )
                        })}
                    </div>
                )}

                {/* Edit Device Dialog */}
                <Dialog open={editOpen} onOpenChange={setEditOpen}>
                    <DialogContent className="sm:max-w-md">
                        <DialogHeader>
                            <DialogTitle>Manage Device</DialogTitle>
                            <DialogDescription>
                                View and edit device settings
                            </DialogDescription>
                        </DialogHeader>
                        {editDevice && (
                            <div className="space-y-4 py-4">
                                <div className="grid gap-2">
                                    <Label htmlFor="editName">Device Name</Label>
                                    <Input
                                        id="editName"
                                        value={editName}
                                        onChange={(e) => setEditName(e.target.value)}
                                    />
                                </div>
                                <div className="grid gap-2">
                                    <Label className="text-muted-foreground">Device ID</Label>
                                    <p className="text-sm font-mono bg-muted p-2 rounded">{editDevice.id}</p>
                                </div>
                                <div className="grid gap-2">
                                    <Label className="text-muted-foreground">Status</Label>
                                    <div className="flex items-center gap-2">
                                        <Badge variant={isOnline(editDevice) ? 'default' : 'secondary'}>
                                            {isOnline(editDevice) ? 'Online' : 'Offline'}
                                        </Badge>
                                        <span className="text-sm text-muted-foreground">
                                            Last seen {formatLastSeen(editDevice.lastSeen)}
                                        </span>
                                    </div>
                                </div>

                                {/* Device Health Stats */}
                                <div className="grid grid-cols-2 gap-3 pt-4 border-t">
                                    <div className="text-center p-3 rounded-lg bg-muted/50">
                                        <p className="text-2xl font-bold">{getUptime(editDevice).toFixed(1)}%</p>
                                        <p className="text-xs text-muted-foreground">Uptime</p>
                                    </div>
                                    <div className="text-center p-3 rounded-lg bg-muted/50">
                                        <p className="text-2xl font-bold">{isOnline(editDevice) ? 'Strong' : '--'}</p>
                                        <p className="text-xs text-muted-foreground">Signal</p>
                                    </div>
                                </div>
                            </div>
                        )}
                        <DialogFooter className="flex-col sm:flex-row gap-2">
                            <NativeDelete
                                buttonText="Unregister"
                                confirmText="Confirm"
                                size="md"
                                onConfirm={() => { }}
                                onDelete={() => {
                                    if (editDevice) handleUnregister(editDevice.id)
                                    setEditOpen(false)
                                }}
                            />
                            <div className="flex-1" />
                            <Button variant="outline" onClick={() => setEditOpen(false)}>
                                Cancel
                            </Button>
                            <Button onClick={handleEditSave}>
                                Save Changes
                            </Button>
                        </DialogFooter>
                    </DialogContent>
                </Dialog>
            </Main>
        </>
    )
}
