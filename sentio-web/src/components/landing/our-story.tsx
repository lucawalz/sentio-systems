import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { SplitText } from "gsap/SplitText"

const milestones = [
  {
    year: "2023",
    title: "The Vision",
    description:
        "Conceived the revolutionary idea of empowering everyday people to become citizen scientists, creating a global network of backyard biodiversity monitoring stations to combat the accelerating wildlife crisis.",
    icon: "💡",
    color: "#B0D6FF",
  },
  {
    year: "2024",
    title: "First Prototype",
    description:
        "Developed and deployed the first Raspberry Pi-based biodiversity monitoring station with AI-powered bird identification, contributing valuable data to local wildlife conservation efforts.",
    icon: "🔬",
    color: "#A8D5BA",
  },
  {
    year: "2024",
    title: "AI Integration",
    description:
        "Successfully integrated advanced machine learning models achieving 95% accuracy in species identification, enabling thousands of users to contribute meaningful data to global biodiversity research.",
    icon: "🧠",
    color: "#FFD8A8",
  },
  {
    year: "2025",
    title: "Sentio Network Launch",
    description:
        "Launched the complete citizen science ecosystem where backyard monitoring stations create a distributed research network, generating both personal nature insights and global conservation data.",
    icon: "🚀",
    color: "#B0D6FF",
  },
  {
    year: "2026",
    title: "Global Expansion",
    description:
        "Scaling to 100,000+ citizen science stations worldwide, partnering with conservation organizations to track migration patterns, climate impacts, and species population trends across all continents.",
    icon: "🌍",
    color: "#A8D5BA",
    isFuture: true,
  },
]

export function OurStory() {
  const sectionRef = useRef<HTMLDivElement>(null)
  const timelineRef = useRef<HTMLDivElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Set initial hidden states
      gsap.set(".timeline-item .timeline-card", { x: 200, opacity: 0, scale: 0.8, rotationY: 45 })
      gsap.set(".timeline-item:nth-child(even) .timeline-card", { x: -200, rotationY: -45 })
      gsap.set(".timeline-icon", { scale: 0, rotation: -180 })
      gsap.set(".timeline-content > *", { y: 30, opacity: 0 })
      gsap.set(".timeline-line", { scaleY: 0, transformOrigin: "top center" })

      // Enhanced header animation with proper trigger and null check
      const h2Element = headerRef.current?.querySelector("h2")
      if (h2Element) {
        const headerSplit = new SplitText(h2Element, { type: "chars,words" })
        gsap.set(headerSplit.chars, { y: 100, opacity: 0, rotationY: 90 })

        gsap.to(headerSplit.chars, {
          y: 0,
          opacity: 1,
          rotationY: 0,
          duration: 1,
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

      // Enhanced timeline animations with better triggers
      const items = gsap.utils.toArray(".timeline-item")

      items.forEach((item: any) => {
        const card = item.querySelector(".timeline-card")
        const icon = item.querySelector(".timeline-icon")
        const content = item.querySelector(".timeline-content")
        const isFuture = item.querySelector(".future-badge")

        // Create timeline with proper scroll sync
        const itemTl = gsap.timeline({
          scrollTrigger: {
            trigger: item,
            start: "top 75%",
            end: "bottom 25%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
        })

        // Card slide and scale
        itemTl.to(card, {
          x: 0,
          opacity: 1,
          scale: 1,
          rotationY: 0,
          duration: 1.2,
          ease: "power3.out",
        })

        // Icon pop-in with bounce
        itemTl.to(
            icon,
            {
              scale: 1,
              rotation: 0,
              duration: 0.8,
              ease: "back.out(2)",
            },
            "-=0.8",
        )

        // Future badge animation
        if (isFuture) {
          itemTl.to(
              isFuture,
              {
                scale: 1,
                opacity: 1,
                duration: 0.6,
                ease: "back.out(1.7)",
              },
              "-=0.4",
          )
        }

        // Content stagger
        const contentElements = content?.children
        if (contentElements) {
          itemTl.to(
              contentElements,
              {
                y: 0,
                opacity: 1,
                duration: 0.6,
                ease: "power2.out",
                stagger: 0.1,
              },
              "-=0.6",
          )
        }

        // Enhanced hover effects for future items
        card?.addEventListener("mouseenter", () => {
          gsap.to(card, {
            scale: 1.02,
            y: -5,
            boxShadow: isFuture ? `0 20px 40px rgba(168,213,186,0.4)` : `0 20px 40px rgba(0,0,0,0.3)`,
            duration: 0.3,
            ease: "power2.out",
          })

          if (isFuture) {
            gsap.to(card.querySelector(".future-glow"), {
              opacity: 0.3,
              scale: 1.1,
              duration: 0.3,
              ease: "power2.out",
            })
          }
        })

        card?.addEventListener("mouseleave", () => {
          gsap.to(card, {
            scale: 1,
            y: 0,
            boxShadow: `0 10px 20px rgba(0,0,0,0.1)`,
            duration: 0.3,
            ease: "power2.out",
          })

          if (isFuture) {
            gsap.to(card.querySelector(".future-glow"), {
              opacity: 0,
              scale: 1,
              duration: 0.3,
              ease: "power2.out",
            })
          }
        })
      })

      // Enhanced timeline line animation with proper scrub
      gsap.to(".timeline-line", {
        scaleY: 1,
        duration: 1,
        ease: "none",
        scrollTrigger: {
          trigger: timelineRef.current,
          start: "top 80%",
          end: "bottom 20%",
          scrub: 1,
          invalidateOnRefresh: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
      <section id="our-story" ref={sectionRef} className="py-40 px-6 bg-gradient-to-b from-black via-gray-900 to-black">
        <div className="max-w-6xl mx-auto">
          <div ref={headerRef} className="text-center mb-32">
            <h2 className="text-6xl md:text-8xl font-bold mb-8 text-white">
                From Local to Global Impact
            </h2>
            <p className="text-2xl text-white/70 max-w-4xl mx-auto leading-relaxed">
                Building distributed intelligence that serves climate science, biodiversity conservation, and community safety
            </p>
          </div>

          <div ref={timelineRef} className="relative">
            {/* Enhanced Timeline Line */}
            <div className="timeline-line absolute left-1/2 transform -translate-x-1/2 w-1 h-full bg-gradient-to-b from-[#B0D6FF] via-[#A8D5BA] to-[#FFD8A8] rounded-full" />

            {milestones.map((milestone, index) => {
              const isEven = index % 2 === 0
              return (
                  <div
                      key={index}
                      className={`timeline-item relative flex items-center mb-32 ${
                          isEven ? "flex-row" : "flex-row-reverse"
                      }`}
                  >
                    <div className={`w-1/2 ${isEven ? "pr-16 text-right" : "pl-16 text-left"}`}>
                      <div
                          className={`timeline-card relative bg-white/5 backdrop-blur-sm border border-white/40 rounded-3xl p-10 shadow-2xl ${
                              milestone.isFuture ? "border-[#A8D5BA]/60 bg-white/10" : ""
                          }`}
                      >
                        {/* Future Badge - Higher z-index */}
                        {milestone.isFuture && (
                            <div className="future-badge absolute -top-3 -right-3 bg-gradient-to-r from-[#A8D5BA] to-[#B0D6FF] text-black px-4 py-2 rounded-full text-sm font-bold shadow-lg scale-0 opacity-0 backdrop-blur-sm z-30">
                              Coming Soon
                            </div>
                        )}

                        {/* Future Glow Effect - Lower z-index */}
                        {milestone.isFuture && (
                            <div className="future-glow absolute inset-0 rounded-3xl bg-gradient-to-br from-[#A8D5BA]/20 to-[#B0D6FF]/20 opacity-0 blur-xl z-0" />
                        )}

                        <div className="timeline-content space-y-6 relative z-20">
                          <div
                              className={`text-2xl font-bold ${milestone.isFuture ? "text-[#A8D5BA]" : ""}`}
                              style={{ color: milestone.isFuture ? milestone.color : milestone.color }}
                          >
                            {milestone.year}
                          </div>
                          <h3 className={`text-3xl font-bold ${milestone.isFuture ? "text-[#A8D5BA]" : "text-white"}`}>
                            {milestone.title}
                          </h3>
                          <p className={`leading-relaxed text-lg ${milestone.isFuture ? "text-white/90" : "text-white/80"}`}>
                            {milestone.description}
                          </p>

                          {/* Future indicator */}
                          {milestone.isFuture && (
                              <div className="flex items-center space-x-2 pt-4 border-t border-[#A8D5BA]/30">
                                <div className="w-2 h-2 bg-[#A8D5BA] rounded-full animate-pulse" />
                                <span className="text-sm text-[#A8D5BA] font-medium">Future Vision</span>
                              </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Enhanced Timeline Dot */}
                    <div
                        className={`timeline-icon absolute left-1/2 transform -translate-x-1/2 w-20 h-20 rounded-full border-4 border-black flex items-center justify-center text-2xl shadow-2xl backdrop-blur-sm ${
                            milestone.isFuture ? "ring-4 ring-[#A8D5BA]/30" : ""
                        }`}
                        style={{ backgroundColor: milestone.color }}
                    >
                      {milestone.icon}
                      {milestone.isFuture && (
                          <div className="absolute inset-0 rounded-full bg-gradient-to-br from-white/20 to-transparent animate-pulse" />
                      )}
                    </div>
                  </div>
              )
            })}
          </div>
        </div>
      </section>
  )
}