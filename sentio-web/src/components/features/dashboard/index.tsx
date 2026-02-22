import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Header } from '@/components/layout/dashboard/header'
import { Main } from '@/components/layout/dashboard/main'
import { useAuth } from '@/context/auth-context'
import { Cpu, Activity, Thermometer, MessageCircle, Send, Bot, Sparkles, Bird, Trash2 } from 'lucide-react'
import { useState, useEffect, useRef, useMemo, useCallback } from 'react'
import { devicesApi, weatherApi, animalsApi, workflowApi, alertsApi } from '@/lib/api'
import type { WeatherStats, AnimalSummary, Device, WeatherAlert, AnimalDetection } from '@/types/api'
import { useAnimatedCounter } from '@/hooks/use-animated-counter'
import { SystemHealth } from './components/system-health'
import { ActivityFeed } from './components/activity-feed'
import { SmartInsightCard } from './components/smart-insight-card'

const CHAT_STORAGE_KEY = 'sentio-ai-chat-messages'

interface ChatMessage {
    role: 'user' | 'assistant'
    content: string
    timestamp: Date
}

// Random personalized greetings
const GREETINGS = [
    () => `Ready to explore today's wildlife?`,
    () => `Your garden has been busy while you were away.`,
    () => `Let's see what nature has in store today.`,
    () => `Here's your personalized nature update.`,
]

// Time-aware prefix
function getTimeGreeting(): string {
    const hour = new Date().getHours()
    if (hour < 12) return 'Good morning'
    if (hour < 17) return 'Good afternoon'
    return 'Good evening'
}

// Simple markdown renderer for chat messages
function renderMarkdown(text: string): React.ReactNode {
    // Split by double newlines for paragraphs
    const paragraphs = text.split(/\n\n+/)

    return paragraphs.map((paragraph, pIndex) => {
        // Check if it's a list
        const lines = paragraph.split('\n')
        const isList = lines.every(line => line.trim().startsWith('*') || line.trim().startsWith('-') || line.trim() === '')

        if (isList && lines.some(line => line.trim().startsWith('*') || line.trim().startsWith('-'))) {
            return (
                <ul key={pIndex} className="list-disc list-inside space-y-1 my-2">
                    {lines.filter(line => line.trim()).map((line, lIndex) => (
                        <li key={lIndex} className="text-sm">
                            {renderInlineMarkdown(line.replace(/^[\*\-]\s*/, ''))}
                        </li>
                    ))}
                </ul>
            )
        }

        // Regular paragraph
        return (
            <p key={pIndex} className="text-sm mb-2 last:mb-0">
                {renderInlineMarkdown(paragraph.replace(/\n/g, ' '))}
            </p>
        )
    })
}

// Handle inline markdown (bold, italic, code)
function renderInlineMarkdown(text: string): React.ReactNode {
    const parts: React.ReactNode[] = []
    let remaining = text
    let key = 0

    while (remaining.length > 0) {
        // Bold **text**
        const boldMatch = remaining.match(/\*\*(.+?)\*\*/)
        // Italic *text*
        const italicMatch = remaining.match(/(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)/)
        // Inline code `text`
        const codeMatch = remaining.match(/`(.+?)`/)

        // Find the earliest match
        const matches = [
            boldMatch && { type: 'bold', match: boldMatch, index: boldMatch.index! },
            italicMatch && { type: 'italic', match: italicMatch, index: italicMatch.index! },
            codeMatch && { type: 'code', match: codeMatch, index: codeMatch.index! },
        ].filter(Boolean).sort((a, b) => a!.index - b!.index)

        if (matches.length === 0) {
            parts.push(remaining)
            break
        }

        const first = matches[0]!

        // Add text before the match
        if (first.index > 0) {
            parts.push(remaining.slice(0, first.index))
        }

        // Add the formatted text
        if (first.type === 'bold') {
            parts.push(<strong key={key++}>{first.match[1]}</strong>)
        } else if (first.type === 'italic') {
            parts.push(<em key={key++}>{first.match[1]}</em>)
        } else if (first.type === 'code') {
            parts.push(<code key={key++} className="bg-muted px-1 py-0.5 rounded text-xs">{first.match[1]}</code>)
        }

        remaining = remaining.slice(first.index + first.match[0].length)
    }

    return parts.length === 1 ? parts[0] : parts
}

export function Dashboard() {
    const { user } = useAuth()
    const [loading, setLoading] = useState(true)
    const [devices, setDevices] = useState<Device[]>([])
    const [weatherStats, setWeatherStats] = useState<WeatherStats | null>(null)
    const [animalSummary, setAnimalSummary] = useState<AnimalSummary | null>(null)
    const [detections, setDetections] = useState<AnimalDetection[]>([])
    const [alerts, setAlerts] = useState<WeatherAlert[]>([])

    // AI Chat state - load from localStorage
    const [messages, setMessages] = useState<ChatMessage[]>(() => {
        if (typeof window !== 'undefined') {
            const saved = localStorage.getItem(CHAT_STORAGE_KEY)
            if (saved) {
                try {
                    const parsed = JSON.parse(saved)
                    return parsed.map((m: ChatMessage) => ({
                        ...m,
                        timestamp: new Date(m.timestamp)
                    }))
                } catch {
                    return []
                }
            }
        }
        return []
    })
    const [input, setInput] = useState('')
    const [sending, setSending] = useState(false)
    const chatEndRef = useRef<HTMLDivElement>(null)

    // Persist messages to localStorage
    useEffect(() => {
        localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages))
    }, [messages])

    const clearChat = useCallback(() => {
        setMessages([])
        localStorage.removeItem(CHAT_STORAGE_KEY)
    }, [])

    // Animated counters
    const animatedDetections = useAnimatedCounter(animalSummary?.totalDetections || 0, { delay: 300 })
    const animatedSpecies = useAnimatedCounter(animalSummary?.uniqueSpecies || 0, { delay: 500 })
    const animatedTemp = useAnimatedCounter(
        weatherStats?.latest?.temperature || 0,
        { delay: 400, decimals: 1 }
    )

    // Pick a random greeting once per session
    const greeting = useMemo(() => {
        const randomGreeting = GREETINGS[Math.floor(Math.random() * GREETINGS.length)]
        return randomGreeting()
    }, [])

    const timeGreeting = useMemo(() => getTimeGreeting(), [])

    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true)
                const [devicesRes, weatherRes, animalsRes, detectionsRes, alertsRes] = await Promise.allSettled([
                    devicesApi.list(),
                    weatherApi.stats(),
                    animalsApi.summary(24),
                    animalsApi.latest(15),
                    alertsApi.currentLocation(),
                ])

                if (devicesRes.status === 'fulfilled') {
                    const data = devicesRes.value.data
                    setDevices(Array.isArray(data) ? data : [])
                }
                if (weatherRes.status === 'fulfilled') setWeatherStats(weatherRes.value.data)
                if (animalsRes.status === 'fulfilled') setAnimalSummary(animalsRes.value.data)
                if (detectionsRes.status === 'fulfilled') {
                    const data = detectionsRes.value.data
                    setDetections(Array.isArray(data) ? data : [])
                }
                if (alertsRes.status === 'fulfilled') {
                    const data = alertsRes.value.data
                    setAlerts(Array.isArray(data) ? data : [])
                }
            } catch (err) {
                console.error(err)
            } finally {
                setLoading(false)
            }
        }
        fetchData()
    }, [])

    // Scroll chat to bottom when new messages arrive
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [messages])

    const handleSendMessage = async () => {
        if (!input.trim() || sending) return

        const userMessage: ChatMessage = {
            role: 'user',
            content: input.trim(),
            timestamp: new Date(),
        }
        setMessages((prev) => [...prev, userMessage])
        setInput('')
        setSending(true)

        try {
            const response = await workflowApi.askAgent(input.trim())
            const assistantMessage: ChatMessage = {
                role: 'assistant',
                content: response.data.response || 'Sorry, I could not process your request.',
                timestamp: new Date(),
            }
            setMessages((prev) => [...prev, assistantMessage])
        } catch (err) {
            console.error(err)
            const errorMessage: ChatMessage = {
                role: 'assistant',
                content: 'Sorry, there was an error processing your request. Please try again.',
                timestamp: new Date(),
            }
            setMessages((prev) => [...prev, errorMessage])
        } finally {
            setSending(false)
        }
    }

    const onlineDevices = Array.isArray(devices) ? devices.filter((d) => {
        if (!d.lastSeen) return false
        const lastSeenTime = d.lastSeen.endsWith('Z') ? d.lastSeen : `${d.lastSeen}Z`
        return Date.now() - new Date(lastSeenTime).getTime() < 5 * 60 * 1000
    }) : []

    return (
        <>
            <Header>
                <div className="flex items-center gap-2">
                    <Sparkles className="h-5 w-5" />
                    <h1 className="text-lg font-semibold">Home</h1>
                </div>
            </Header>

            <Main>
                {/* Personalized Welcome */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold tracking-tight mb-2">
                        {timeGreeting}, {user?.username || 'there'}! 👋
                    </h1>
                    <p className="text-lg text-muted-foreground">
                        {greeting}
                    </p>
                </div>

                {/* Stats Cards Row - Upgraded with SmartInsightCard */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 w-full mb-6">
                    <SmartInsightCard
                        icon={<Cpu className="h-4 w-4" />}
                        value={loading ? '...' : `${onlineDevices.length} / ${devices.length}`}
                        label="Devices Online"
                        trend={onlineDevices.length === devices.length ? 'up' : 'neutral'}
                        trendValue={onlineDevices.length === devices.length ? 'All online' : `${devices.length - onlineDevices.length} offline`}
                        variant={onlineDevices.length === devices.length ? 'success' : onlineDevices.length > 0 ? 'warning' : 'danger'}
                        loading={loading}
                        expandedContent={
                            <div className="space-y-1 text-xs">
                                {devices.slice(0, 3).map((d) => (
                                    <div key={d.id} className="flex items-center justify-between">
                                        <span className="truncate">{d.name}</span>
                                        <span className={onlineDevices.some(od => od.id === d.id) ? 'text-green-500' : 'text-muted-foreground'}>
                                            {onlineDevices.some(od => od.id === d.id) ? '● Online' : '○ Offline'}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        }
                    />

                    <SmartInsightCard
                        icon={<Activity className="h-4 w-4" />}
                        value={loading ? '...' : animatedDetections.toString()}
                        label="Today's Detections"
                        trend={(animalSummary?.totalDetections || 0) > 10 ? 'up' : 'neutral'}
                        trendValue={`${animatedSpecies} species`}
                        loading={loading}
                        expandedContent={
                            <div className="space-y-1 text-xs">
                                <div className="flex items-center gap-1">
                                    <Bird className="h-3 w-3" />
                                    <span>{animatedSpecies} unique species spotted</span>
                                </div>
                                <div className="text-muted-foreground">
                                    In the last 24 hours
                                </div>
                            </div>
                        }
                    />

                    <SmartInsightCard
                        icon={<Thermometer className="h-4 w-4" />}
                        value={loading ? '...' : weatherStats?.latest?.temperature != null ? `${animatedTemp}°C` : '--'}
                        label="Current Temperature"
                        trend={
                            weatherStats?.latest?.temperature != null
                                ? weatherStats.latest.temperature > 25 ? 'up' : weatherStats.latest.temperature < 10 ? 'down' : 'neutral'
                                : 'neutral'
                        }
                        trendValue={weatherStats?.latest?.humidity != null ? `${weatherStats.latest.humidity.toFixed(0)}% humidity` : '--'}
                        variant={
                            weatherStats?.latest?.temperature != null
                                ? weatherStats.latest.temperature > 30 ? 'danger' : weatherStats.latest.temperature < 5 ? 'warning' : 'default'
                                : 'default'
                        }
                        loading={loading}
                        expandedContent={
                            weatherStats?.latest && (
                                <div className="grid grid-cols-2 gap-2 text-xs">
                                    <div>
                                        <span className="text-muted-foreground">Pressure:</span>{' '}
                                        {weatherStats.latest.pressure?.toFixed(0) ?? '--'} hPa
                                    </div>
                                    <div>
                                        <span className="text-muted-foreground">Humidity:</span>{' '}
                                        {weatherStats.latest.humidity?.toFixed(0) ?? '--'}%
                                    </div>
                                </div>
                            )
                        }
                    />

                    <SmartInsightCard
                        icon={<Bot className="h-4 w-4" />}
                        value="Active"
                        label="AI Agent Status"
                        trend="up"
                        trendValue="Ready"
                        variant="success"
                        loading={loading}
                        expandedContent={
                            <div className="text-xs space-y-1">
                                <div className="flex items-center gap-1">
                                    <Sparkles className="h-3 w-3 text-primary" />
                                    <span>Powered by n8n + AI</span>
                                </div>
                                <div className="text-muted-foreground">
                                    Ask about weather, detections, and devices
                                </div>
                            </div>
                        }
                    />
                </div>

                {/* Main Content: Asymmetric 7-column grid layout */}
                <div className="grid grid-cols-1 gap-4 lg:grid-cols-7">
                    {/* AI Chat - 5/7 columns */}
                    <Card className="col-span-1 lg:col-span-5 min-h-[650px] flex flex-col">
                        <CardHeader className="pb-3">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <MessageCircle className="h-5 w-5" />
                                    <CardTitle>AI Agent</CardTitle>
                                </div>
                                {messages.length > 0 && (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={clearChat}
                                        className="text-muted-foreground hover:text-destructive"
                                    >
                                        <Trash2 className="h-4 w-4 mr-1" />
                                        Clear
                                    </Button>
                                )}
                            </div>
                            <CardDescription>
                                Ask about weather, detections, and devices
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="flex flex-col flex-1">
                            {/* Chat Messages */}
                            <div className="flex-1 overflow-y-auto border rounded-lg p-4 mb-4 bg-muted/30 max-h-[450px]">
                                {messages.length === 0 ? (
                                    <div className="flex flex-col items-center justify-center h-full text-center text-muted-foreground">
                                        <Bot className="h-10 w-10 mb-3" />
                                        <p className="font-medium">Ask me anything!</p>
                                        <div className="flex flex-wrap justify-center gap-2 mt-3">
                                            <button
                                                onClick={() => setInput("What's the weather like?")}
                                                className="px-3 py-1.5 text-xs bg-muted hover:bg-muted/80 rounded-full transition-colors"
                                            >
                                                Weather?
                                            </button>
                                            <button
                                                onClick={() => setInput("How many birds today?")}
                                                className="px-3 py-1.5 text-xs bg-muted hover:bg-muted/80 rounded-full transition-colors"
                                            >
                                                Birds today?
                                            </button>
                                            <button
                                                onClick={() => setInput("Activity summary")}
                                                className="px-3 py-1.5 text-xs bg-muted hover:bg-muted/80 rounded-full transition-colors"
                                            >
                                                Summary
                                            </button>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="space-y-3">
                                        {messages.map((msg, i) => (
                                            <div
                                                key={i}
                                                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                                            >
                                                <div
                                                    className={`max-w-[80%] rounded-lg px-3 py-2 ${msg.role === 'user'
                                                        ? 'bg-primary text-primary-foreground'
                                                        : 'bg-muted'
                                                        }`}
                                                >
                                                    {msg.role === 'user' ? (
                                                        <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                                                    ) : (
                                                        <div className="prose prose-sm dark:prose-invert max-w-none">
                                                            {renderMarkdown(msg.content)}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                        {sending && (
                                            <div className="flex justify-start">
                                                <div className="bg-muted rounded-lg px-3 py-2">
                                                    <div className="flex gap-1">
                                                        <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce" />
                                                        <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce delay-100" />
                                                        <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce delay-200" />
                                                    </div>
                                                </div>
                                            </div>
                                        )}
                                        <div ref={chatEndRef} />
                                    </div>
                                )}
                            </div>

                            {/* Chat Input */}
                            <div className="flex gap-2">
                                <input
                                    type="text"
                                    value={input}
                                    onChange={(e) => setInput(e.target.value)}
                                    onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                                    placeholder="Ask the AI agent..."
                                    className="flex-1 px-3 py-2 border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary text-sm"
                                    disabled={sending}
                                />
                                <Button onClick={handleSendMessage} disabled={sending || !input.trim()}>
                                    <Send className="h-4 w-4" />
                                </Button>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Sidebar - 2/7 columns */}
                    <div className="col-span-1 lg:col-span-2 flex flex-col gap-4">
                        <div className="flex-1">
                            <ActivityFeed
                                detections={detections}
                                alerts={alerts}
                                devices={devices}
                                loading={loading}
                            />
                        </div>
                        <div className="flex-1">
                            <SystemHealth
                                devices={devices}
                                loading={loading}
                            />
                        </div>
                    </div>
                </div>
            </Main>
        </>
    )
}
