import { useEffect, useRef, useState } from "react"
import { gsap } from "gsap"
import { SplitText } from "gsap/SplitText"
import { Cpu, Camera, Truck, Network, ChevronLeft, ChevronRight, Shield, Zap, Eye, AlertTriangle } from "lucide-react"

const products = [
    {
        id: "core",
        icon: Cpu,
        name: "Orbis Core",
        tagline: "The Community Sentinel",
        description: "Advanced intelligence hub delivering 2-6 hour early warnings for flash floods, wildfire detection at 0.1-0.5 acres, and comprehensive environmental monitoring.",
        keyFeatures: [
            "26 TOPS AI processing power",
            "Flash flood prediction system",
            "Wildfire detection & spread modeling",
            "Air quality & extreme heat alerts",
            "72-hour battery backup"
        ],
        specs: {
            "Processing": "Raspberry Pi 5 + AI Hat",
            "Sensors": "Climate, disaster-specific, imaging",
            "Range": "10km lightning detection",
            "Durability": "IP67, -40°C to +70°C"
        },
        pricing: "€899 - €1,599",
        gradient: "from-blue-500 to-cyan-400",
        bgColor: "bg-gradient-to-br from-blue-500/20 to-cyan-400/20"
    },
    {
        id: "aether",
        icon: Camera,
        name: "Orbis Aether",
        tagline: "Eyes in the Sky",
        description: "Autonomous aerial platform for disaster reconnaissance, real-time fire mapping, flood assessment, and search & rescue operations with 60-minute flight time.",
        keyFeatures: [
            "60-minute autonomous flight",
            "4K + thermal + LiDAR imaging",
            "Real-time fire perimeter mapping",
            "Search & rescue thermal detection",
            "Emergency supply delivery"
        ],
        specs: {
            "Flight Time": "60 minutes",
            "Range": "50km operational radius",
            "Weather": "Light rain, 40km/h winds",
            "Imaging": "4K, thermal, multispectral, LiDAR"
        },
        pricing: "€2,499 - €4,999",
        gradient: "from-green-500 to-emerald-400",
        bgColor: "bg-gradient-to-br from-green-500/20 to-emerald-400/20"
    },
    {
        id: "strata",
        icon: Truck,
        name: "Orbis Strata",
        tagline: "Ground Intelligence",
        description: "All-terrain rover for ground-level risk assessment, infrastructure inspection, hazmat detection, and soil stability analysis with autonomous navigation.",
        keyFeatures: [
            "8-hour autonomous operation",
            "Ground-penetrating radar",
            "Chemical & radiation detection",
            "Infrastructure integrity assessment",
            "Emergency sample collection"
        ],
        specs: {
            "Operation": "8-hour autonomous runtime",
            "Navigation": "GPS + LiDAR + vision",
            "Detection": "Multi-gas, soil, water analysis",
            "Depth": "5m ground penetration"
        },
        pricing: "€1,699 - €2,899",
        gradient: "from-orange-500 to-amber-400",
        bgColor: "bg-gradient-to-br from-orange-500/20 to-amber-400/20"
    },
    {
        id: "lumen",
        icon: Network,
        name: "Orbis Lumen",
        tagline: "The Detection Web",
        description: "Ultra-lightweight distributed sensor network creating comprehensive early warning coverage with 2-year battery life and mesh networking.",
        keyFeatures: [
            "2-year battery life",
            "10km LoRaWAN range",
            "Mesh network resilience",
            "AI threat classification",
            "Infrastructure-independent operation"
        ],
        specs: {
            "Battery": "2-year life + solar charging",
            "Range": "10km mesh networking",
            "Durability": "IP68 waterproofing",
            "Network": "Self-healing mesh topology"
        },
        pricing: "€199 - €349/node",
        gradient: "from-purple-500 to-pink-400",
        bgColor: "bg-gradient-to-br from-purple-500/20 to-pink-400/20"
    }
]

const capabilities = [
    { icon: Shield, text: "Disaster Prevention" },
    { icon: Eye, text: "Real-time Monitoring" },
    { icon: AlertTriangle, text: "Early Warning" },
    { icon: Zap, text: "Autonomous Response" }
]

export function ProductOverview() {
    const [activeIndex, setActiveIndex] = useState(0)
    const [isAutoPlaying, setIsAutoPlaying] = useState(true)
    const sectionRef = useRef<HTMLDivElement>(null)
    const headerRef = useRef<HTMLDivElement>(null)
    const carouselRef = useRef<HTMLDivElement>(null)
    const autoPlayRef = useRef<number | null>(null)

    const nextProduct = () => {
        setActiveIndex((prev) => (prev + 1) % products.length)
    }

    const prevProduct = () => {
        setActiveIndex((prev) => (prev - 1 + products.length) % products.length)
    }

    const goToProduct = (index: number) => {
        setActiveIndex(index)
        setIsAutoPlaying(false)
    }

    useEffect(() => {
        if (isAutoPlaying) {
            autoPlayRef.current = setInterval(nextProduct, 5000)
        } else if (autoPlayRef.current !== null) {
            clearInterval(autoPlayRef.current)
            autoPlayRef.current = null
        }

        return () => {
            if (autoPlayRef.current !== null) {
                clearInterval(autoPlayRef.current)
                autoPlayRef.current = null
            }
        }
    }, [isAutoPlaying])

    useEffect(() => {
        const ctx = gsap.context(() => {
            // Header animation
            const h2Element = headerRef.current?.querySelector("h2")
            if (h2Element) {
                const headerSplit = new SplitText(h2Element, { type: "chars" })
                gsap.set(headerSplit.chars, { y: 100, opacity: 0, rotationX: -90 })

                gsap.to(headerSplit.chars, {
                    y: 0,
                    opacity: 1,
                    rotationX: 0,
                    duration: 1.2,
                    ease: "back.out(1.7)",
                    stagger: 0.05,
                    scrollTrigger: {
                        trigger: headerRef.current,
                        start: "top 70%",
                        toggleActions: "play none none reverse",
                    },
                })
            }

            const pElement = headerRef.current?.querySelector("p")
            if (pElement) {
                gsap.set(pElement, { y: 50, opacity: 0 })
                gsap.to(pElement, {
                    y: 0,
                    opacity: 1,
                    duration: 1,
                    ease: "power3.out",
                    delay: 0.3,
                    scrollTrigger: {
                        trigger: headerRef.current,
                        start: "top 70%",
                        toggleActions: "play none none reverse",
                    },
                })
            }

            // Carousel animation
            gsap.set(".carousel-container", { y: 100, opacity: 0 })
            gsap.to(".carousel-container", {
                y: 0,
                opacity: 1,
                duration: 1.5,
                ease: "power3.out",
                delay: 0.5,
                scrollTrigger: {
                    trigger: carouselRef.current,
                    start: "top 70%",
                    toggleActions: "play none none reverse",
                },
            })
        }, sectionRef)

        return () => ctx.revert()
    }, [])

    const activeProduct = products[activeIndex]

    return (
        <section
            id="products"
            ref={sectionRef}
            className="py-40 px-6 bg-gradient-radial from-gray-900 via-black to-gray-900 overflow-hidden"
        >
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div ref={headerRef} className="text-center mb-20">
                    <h2 className="text-6xl md:text-8xl font-bold mb-8 text-white">
                        Product Ecosystem
                    </h2>
                    <p className="text-2xl text-white/70 max-w-4xl mx-auto leading-relaxed">
                        Four integrated platforms delivering comprehensive disaster prevention and environmental intelligence
                    </p>
                </div>

                {/* Capabilities Strip */}
                <div className="flex justify-center mb-16">
                    <div className="flex items-center gap-8 glass rounded-full px-8 py-4 border border-white/20">
                        {capabilities.map((capability, index) => (
                            <div key={index} className="flex items-center gap-3 text-white/80">
                                <capability.icon className="w-5 h-5" />
                                <span className="text-sm font-medium">{capability.text}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Main Carousel */}
                <div ref={carouselRef} className="carousel-container">
                    <div className="relative">
                        {/* Product Display */}
                        <div className={`${activeProduct.bgColor} rounded-3xl border border-white/20 backdrop-blur-xl overflow-hidden`}>
                            <div className="grid lg:grid-cols-2 gap-0 min-h-[600px]">
                                {/* Left Side - Product Info */}
                                <div className="p-12 flex flex-col justify-center space-y-8">
                                    <div className="flex items-center gap-4">
                                        <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${activeProduct.gradient} flex items-center justify-center shadow-lg`}>
                                            <activeProduct.icon className="w-8 h-8 text-white" />
                                        </div>
                                        <div>
                                            <h3 className="text-4xl font-bold text-white">{activeProduct.name}</h3>
                                            <p className="text-xl text-white/60">{activeProduct.tagline}</p>
                                        </div>
                                    </div>

                                    <p className="text-lg text-white/90 leading-relaxed">
                                        {activeProduct.description}
                                    </p>

                                    <div className="space-y-4">
                                        <h4 className="text-xl font-semibold text-white">Key Features</h4>
                                        <div className="grid gap-3">
                                            {activeProduct.keyFeatures.map((feature, index) => (
                                                <div key={index} className="flex items-center gap-3">
                                                    <div className="w-2 h-2 rounded-full bg-white/60" />
                                                    <span className="text-white/80">{feature}</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>

                                    <div className="flex items-center justify-between pt-4">
                                        <div className="text-3xl font-bold text-white">{activeProduct.pricing}</div>
                                        <button className={`px-8 py-3 rounded-full bg-gradient-to-r ${activeProduct.gradient} text-white font-semibold hover:shadow-xl transition-all duration-300 transform hover:scale-105`}>
                                            Learn More
                                        </button>
                                    </div>
                                </div>

                                {/* Right Side - Specs */}
                                <div className="p-12 bg-black/20 flex flex-col justify-center">
                                    <h4 className="text-2xl font-bold text-white mb-8">Technical Specifications</h4>
                                    <div className="space-y-6">
                                        {Object.entries(activeProduct.specs).map(([key, value], index) => (
                                            <div key={index} className="glass rounded-xl p-4 border border-white/10">
                                                <div className="text-white/60 text-sm font-medium mb-1">{key}</div>
                                                <div className="text-white font-semibold">{value}</div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Navigation Controls */}
                        <div className="absolute top-1/2 -translate-y-1/2 -left-6">
                            <button
                                onClick={prevProduct}
                                onMouseEnter={() => setIsAutoPlaying(false)}
                                className="w-12 h-12 rounded-full glass border border-white/20 flex items-center justify-center text-white hover:bg-white/10 transition-all duration-300 transform hover:scale-110"
                            >
                                <ChevronLeft className="w-6 h-6" />
                            </button>
                        </div>
                        <div className="absolute top-1/2 -translate-y-1/2 -right-6">
                            <button
                                onClick={nextProduct}
                                onMouseEnter={() => setIsAutoPlaying(false)}
                                className="w-12 h-12 rounded-full glass border border-white/20 flex items-center justify-center text-white hover:bg-white/10 transition-all duration-300 transform hover:scale-110"
                            >
                                <ChevronRight className="w-6 h-6" />
                            </button>
                        </div>
                    </div>

                    {/* Product Indicators */}
                    <div className="flex justify-center mt-12 gap-4">
                        {products.map((product, index) => (
                            <button
                                key={index}
                                onClick={() => goToProduct(index)}
                                className={`group flex items-center gap-3 px-4 py-3 rounded-full transition-all duration-300 ${
                                    index === activeIndex
                                        ? `bg-gradient-to-r ${product.gradient} shadow-lg scale-105`
                                        : 'glass border border-white/20 hover:bg-white/10'
                                }`}
                            >
                                <product.icon className={`w-5 h-5 ${index === activeIndex ? 'text-white' : 'text-white/60'}`} />
                                <span className={`text-sm font-medium ${index === activeIndex ? 'text-white' : 'text-white/60'}`}>
                                    {product.name}
                                </span>
                            </button>
                        ))}
                    </div>

                    {/* Progress Bar */}
                    <div className="flex justify-center mt-8">
                        <div className="w-64 h-1 bg-white/20 rounded-full overflow-hidden">
                            <div
                                className={`h-full bg-gradient-to-r ${activeProduct.gradient} transition-all duration-300 ease-out`}
                                style={{ width: `${((activeIndex + 1) / products.length) * 100}%` }}
                            />
                        </div>
                    </div>
                </div>

                {/* Bottom CTA */}
                <div className="text-center mt-20">
                    <div className="glass rounded-2xl p-8 border border-white/20 inline-block">
                        <p className="text-white/80 mb-4">Ready to deploy comprehensive disaster prevention?</p>
                        <button className="px-8 py-3 rounded-full bg-gradient-to-r from-white to-gray-200 text-black font-semibold hover:shadow-xl transition-all duration-300 transform hover:scale-105">
                            Get Complete System Quote
                        </button>
                    </div>
                </div>
            </div>
        </section>
    )
}