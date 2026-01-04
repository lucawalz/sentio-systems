import { ChevronsUpDown, Plus, MapPin, Star, Home } from 'lucide-react'
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuShortcut,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    useSidebar,
} from '@/components/ui/sidebar'
import { LogoIcon } from '@/components/ui/logo'
import { useDevices } from '@/context/device-context'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useState } from 'react'

export function DeviceSwitcher() {
    const { isMobile } = useSidebar()
    const { devices, selectedDevice, setSelectedDevice, setPrimaryDevice, refreshDevices } = useDevices()
    const navigate = useNavigate()
    const [settingPrimary, setSettingPrimary] = useState<string | null>(null)

    // Ensure devices is always an array
    const safeDevices = Array.isArray(devices) ? devices : []

    // Display info based on selection
    const displayName = selectedDevice ? selectedDevice.name : 'Sentio Systems'
    const displaySubtitle = selectedDevice
        ? (selectedDevice.isPrimary ? 'Primary Device' : 'Device View')
        : 'Unified View'

    const handleSelectUnified = () => {
        setSelectedDevice(null)
    }

    const handleSelectDevice = (device: typeof devices[0]) => {
        setSelectedDevice(device)
    }

    const handleSetPrimary = async (e: React.MouseEvent, deviceId: string) => {
        e.stopPropagation()
        e.preventDefault()
        setSettingPrimary(deviceId)
        try {
            await setPrimaryDevice(deviceId)
            await refreshDevices()
        } catch (err) {
            console.error('Failed to set primary device:', err)
        } finally {
            setSettingPrimary(null)
        }
    }

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton
                            size='lg'
                            className='data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground'
                        >
                            <div className='flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground'>
                                {selectedDevice ? (
                                    <MapPin className='size-5' />
                                ) : (
                                    <LogoIcon className='size-5' />
                                )}
                            </div>
                            <div className='grid flex-1 text-start text-sm leading-tight'>
                                <span className='truncate font-semibold'>
                                    {displayName}
                                </span>
                                <span className='truncate text-xs'>{displaySubtitle}</span>
                            </div>
                            <ChevronsUpDown className='ms-auto' />
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        className='w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg'
                        align='start'
                        side={isMobile ? 'bottom' : 'right'}
                        sideOffset={4}
                    >
                        {/* Views Section */}
                        <DropdownMenuLabel className='text-xs text-muted-foreground'>
                            Views
                        </DropdownMenuLabel>
                        <DropdownMenuItem
                            onClick={handleSelectUnified}
                            className='gap-2 p-2'
                        >
                            <div className='flex size-6 items-center justify-center rounded-sm border'>
                                <Home className='size-4' />
                            </div>
                            Sentio Systems
                            {!selectedDevice && (
                                <span className='ml-auto text-xs text-muted-foreground'>Active</span>
                            )}
                            <DropdownMenuShortcut>⌘1</DropdownMenuShortcut>
                        </DropdownMenuItem>

                        {/* Devices Section */}
                        {safeDevices.length > 0 && (
                            <>
                                <DropdownMenuSeparator />
                                <DropdownMenuLabel className='text-xs text-muted-foreground'>
                                    Devices
                                </DropdownMenuLabel>
                                {safeDevices.map((device, index) => (
                                    <DropdownMenuItem
                                        key={device.id}
                                        onClick={() => handleSelectDevice(device)}
                                        className='gap-2 p-2'
                                    >
                                        <div className='flex size-6 items-center justify-center rounded-sm border'>
                                            <MapPin className='size-4' />
                                        </div>
                                        <span className='flex-1 truncate'>{device.name}</span>
                                        {/* Star button to set as primary */}
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className={`h-6 w-6 ${device.isPrimary
                                                ? 'text-yellow-500'
                                                : 'text-muted-foreground hover:text-yellow-500'
                                                }`}
                                            onClick={(e) => handleSetPrimary(e, device.id)}
                                            disabled={settingPrimary === device.id || device.isPrimary}
                                            title={device.isPrimary ? 'Primary device' : 'Set as primary'}
                                        >
                                            <Star className={`size-4 ${device.isPrimary ? 'fill-yellow-500' : ''}`} />
                                        </Button>
                                        {selectedDevice?.id === device.id && (
                                            <span className='text-xs text-muted-foreground'>Active</span>
                                        )}
                                        <DropdownMenuShortcut>⌘{index + 2}</DropdownMenuShortcut>
                                    </DropdownMenuItem>
                                ))}
                            </>
                        )}

                        {/* Register Device */}
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                            className='gap-2 p-2'
                            onClick={() => navigate('/dashboard/devices')}
                        >
                            <div className='flex size-6 items-center justify-center rounded-md border bg-background'>
                                <Plus className='size-4' />
                            </div>
                            <div className='font-medium text-muted-foreground'>Register device</div>
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    )
}
