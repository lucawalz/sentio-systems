import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { SplitText } from "gsap/SplitText"
import { Zap, Eye, Brain, Wifi, Shield, Globe } from "lucide-react"

const features = [
  {
    icon: Zap,
    title: "Climate Intelligence",
    description:
        "Hyperlocal climate monitoring contributing to global research while tracking temperature, precipitation, and ecosystem changes in your area",
    stats: ["100m resolution", "Global network", "Climate tracking"],
    color: "#B0D6FF",
  },
  {
    icon: Eye,
    title: "Biodiversity Monitoring",
    description:
        "AI species identification and ecosystem health tracking that contributes to conservation science and citizen research projects",
    stats: ["400+ species", "95% accuracy", "Conservation data"],
    color: "#A8D5BA",
  },
  {
    icon: Brain,
    title: "Disaster Prevention",
    description:
        "Early warning system for wildfires, floods, and extreme weather with 2-6 hours advance notice and emergency service integration",
    stats: ["2-6 hour warning", "90% accuracy", "Lives protected"],
    color: "#FFD8A8",
  },
  {
    icon: Wifi,
    title: "Global Network",
    description:
        "Connected ecosystem where every device contributes to worldwide environmental intelligence and climate research datasets",
    stats: ["Worldwide data", "Research grade", "Connected science"],
    color: "#B0D6FF",
  },
  {
    icon: Shield,
    title: "Autonomous Response",
    description:
        "Smart interventions from emergency alerts to habitat management recommendations based on real-time environmental conditions",
    stats: ["Auto response", "Smart alerts", "Habitat insights"],
    color: "#A8D5BA",
  },
  {
    icon: Globe,
    title: "Scientific Impact",
    description:
        "Contribute to peer-reviewed research while monitoring your local environment and supporting global conservation efforts",
    stats: ["Research grade", "Peer reviewed", "Global impact"],
    color: "#FFD8A8",
  },
]

export function Features() {
  const sectionRef = useRef<HTMLDivElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)
  const gridRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Set initial hidden states
      gsap.set(".feature-card", { scale: 0.6, opacity: 0, rotateY: 60, z: -100 })
      gsap.set(".feature-icon", { scale: 0, rotation: -180 })
      gsap.set(".feature-stats > *", { x: -30, opacity: 0 })

      // Get elements with null checks
      const h2Element = headerRef.current?.querySelector("h2")
      const pElement = headerRef.current?.querySelector("p")

      if (pElement) {
        gsap.set(pElement, { y: 50, opacity: 0 })
      }

      // Enhanced header animation
      if (h2Element) {
        const headerSplit = new SplitText(h2Element, { type: "chars,words" })
        gsap.set(headerSplit.chars, { y: 100, opacity: 0, rotationX: -90 })

        gsap.to(headerSplit.chars, {
          y: 0,
          opacity: 1,
          rotationX: 0,
          duration: 1.2,
          ease: "back.out(1.7)",
          stagger: 0.03,
          scrollTrigger: {
            trigger: headerRef.current,
            start: "top 70%",
            end: "bottom 30%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
        })
      }

      // Subtitle animation with proper trigger
      if (pElement) {
        gsap.to(pElement, {
          y: 0,
          opacity: 1,
          duration: 1,
          ease: "power3.out",
          scrollTrigger: {
            trigger: headerRef.current,
            start: "top 70%",
            end: "bottom 30%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
          delay: 0.3,
        })
      }

      // Enhanced feature card animations with proper triggers
      const featureCards = gsap.utils.toArray(".feature-card")

      featureCards.forEach((card: any, index) => {
        const cardIcon = card.querySelector(".feature-icon")
        const cardStats = card.querySelector(".feature-stats")

        // Main card entrance with better trigger timing
        gsap.to(card, {
          scale: 1,
          opacity: 1,
          rotateY: 0,
          z: 0,
          duration: 1.8,
          ease: "back.out(1.2)",
          scrollTrigger: {
            trigger: card,
            start: "top 75%",
            end: "bottom 25%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
          delay: index * 0.1,
        })

        // Icon scale and glow animation
        gsap.to(cardIcon, {
          scale: 1,
          rotation: 0,
          duration: 1.2,
          ease: "back.out(2)",
          scrollTrigger: {
            trigger: card,
            start: "top 75%",
            end: "bottom 25%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
          delay: index * 0.1 + 0.3,
        })

        // Stats animation with same trigger
        if (cardStats) {
          gsap.to(cardStats.children, {
            x: 0,
            opacity: 1,
            duration: 0.8,
            ease: "power2.out",
            stagger: 0.1,
            scrollTrigger: {
              trigger: card,
              start: "top 75%",
              end: "bottom 25%",
              toggleActions: "play none none reverse",
              invalidateOnRefresh: true,
            },
            delay: index * 0.1 + 0.5,
          })
        }

        // Enhanced hover effects with 3D transforms
        card.addEventListener("mouseenter", () => {
          gsap.to(card, {
            y: -15,
            scale: 1.03,
            rotateX: 5,
            rotateY: 5,
            z: 50,
            boxShadow: `0 25px 50px rgba(0,0,0,0.3)`,
            duration: 0.4,
            ease: "power2.out",
          })

          gsap.to(cardIcon, {
            scale: 1.1,
            rotation: 5,
            duration: 0.3,
            ease: "back.out(1.7)",
          })

          // Glow effect
          gsap.to(card.querySelector(".feature-glow"), {
            opacity: 0.2,
            scale: 1.1,
            duration: 0.4,
            ease: "power2.out",
          })
        })

        card.addEventListener("mouseleave", () => {
          gsap.to(card, {
            y: 0,
            scale: 1,
            rotateX: 0,
            rotateY: 0,
            z: 0,
            boxShadow: `0 10px 30px rgba(0,0,0,0.2)`,
            duration: 0.4,
            ease: "power2.out",
          })

          gsap.to(cardIcon, {
            scale: 1,
            rotation: 0,
            duration: 0.3,
            ease: "power2.out",
          })

          gsap.to(card.querySelector(".feature-glow"), {
            opacity: 0,
            scale: 1,
            duration: 0.4,
            ease: "power2.out",
          })
        })
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
      <section id="features" ref={sectionRef} className="py-40 px-6 bg-gradient-to-b from-black via-gray-900 to-black">
        <div className="max-w-7xl mx-auto">
          <div ref={headerRef} className="text-center mb-32">
            <h2 className="text-6xl md:text-8xl font-bold mb-8 text-white">
                Environmental Intelligence
            </h2>

            <p className="text-2xl text-white/70 max-w-4xl mx-auto leading-relaxed">
                Advanced monitoring that serves climate science, biodiversity research, and community protection simultaneously
            </p>
          </div>

          <div ref={gridRef} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
                <div key={index} className="feature-card group relative">
                  {/* Glow Effect */}
                  <div
                      className="feature-glow absolute inset-0 rounded-3xl opacity-0 blur-lg transition-all duration-500"
                      style={{ backgroundColor: feature.color }}
                  />

                  <div className="relative glass border border-white/20 rounded-3xl p-10 h-full transition-all duration-500 shadow-2xl">
                    <div className="feature-content space-y-6">
                      <div className="feature-icon">
                        <div
                            className="w-24 h-24 rounded-2xl flex items-center justify-center mb-6 shadow-lg transition-all duration-300"
                            style={{ backgroundColor: feature.color }}
                        >
                          <feature.icon className="w-12 h-12 text-black" />
                        </div>
                      </div>

                      <h3 className="text-2xl font-bold text-white group-hover:text-white transition-colors">
                        {feature.title}
                      </h3>

                      <p className="text-white/80 leading-relaxed text-lg">{feature.description}</p>

                      <div className="feature-stats space-y-3 pt-4 border-t border-white/10">
                        {feature.stats.map((stat, statIndex) => (
                            <div key={statIndex} className="flex items-center space-x-3">
                              <div className="w-2 h-2 rounded-full" style={{ backgroundColor: feature.color }} />
                              <span className="text-white/70 text-sm font-medium">{stat}</span>
                            </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
            ))}
          </div>
        </div>
      </section>
  )
}