import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { ScrollTrigger } from "gsap/ScrollTrigger"
import { SplitText } from "gsap/SplitText"
import { Button } from "../ui/button"
import { ArrowRight, Sparkles } from "lucide-react"
import { Link } from "react-router-dom"
import { useAuth } from "../../context/auth"
// NOTE (added): This CTA was modified to simulate login for development.
// What I changed: The primary CTA shows a "Login" button when not logged in
// and will call `login()` from the auth provider to toggle the state.
//
// Where to place the real login screen link:
// - Replace `onClick={() => login()}` below with a navigation to your
//   login page (e.g. `navigate('/login')`) or open your login modal.
// - Create `src/pages/Login.tsx` and add a `Route` for it in `src/App.tsx`.

// Register ScrollTrigger plugin
gsap.registerPlugin(ScrollTrigger, SplitText)

export function ExperienceCTA() {
  const sectionRef = useRef<HTMLDivElement>(null)
  const contentRef = useRef<HTMLDivElement>(null)
  const backgroundRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Set initial hidden states with null checks
      const h2Element = contentRef.current?.querySelector("h2")
      const pElement = contentRef.current?.querySelector("p")
      const buttonElement = buttonRef.current?.querySelector("button")

      if (h2Element) {
        const titleSplit = new SplitText(h2Element, { type: "chars,words" })
        gsap.set(titleSplit.chars, { y: 100, opacity: 0, rotationX: -90 })

        // Enhanced title animation with SplitText and proper trigger
        gsap.to(titleSplit.chars, {
          y: 0,
          opacity: 1,
          rotationX: 0,
          duration: 1.2,
          ease: "back.out(1.7)",
          stagger: 0.03,
          scrollTrigger: {
            trigger: contentRef.current,
            start: "top 70%",
            end: "bottom 30%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
        })
      }

      if (pElement) {
        gsap.set(pElement, { y: 50, opacity: 0 })

        // Subtitle animation with proper trigger
        gsap.to(pElement, {
          y: 0,
          opacity: 1,
          duration: 1,
          ease: "power3.out",
          scrollTrigger: {
            trigger: contentRef.current,
            start: "top 70%",
            end: "bottom 30%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
          delay: 0.3,
        })
      }

      // Enhanced button animations with proper trigger
      if (buttonElement) {
        gsap.set(buttonElement, { y: 50, opacity: 0, scale: 0.8 })

        gsap.to(buttonElement, {
          y: 0,
          opacity: 1,
          scale: 1,
          duration: 1,
          ease: "back.out(1.7)",
          scrollTrigger: {
            trigger: buttonRef.current,
            start: "top 85%",
            end: "bottom 15%",
            toggleActions: "play none none reverse",
            invalidateOnRefresh: true,
          },
          delay: 0.2,
        })

        // Add fallback
        setTimeout(() => {
          if (buttonElement) {
            gsap.to(buttonElement, {
              y: 0,
              opacity: 1,
              scale: 1,
              duration: 0.8,
              ease: "power2.out",
            })
          }
        }, 2000)

        // Enhanced hover effects with site blur and darken
        buttonElement.addEventListener("mouseenter", () => {
          // Scale and enhance the button
          gsap.to(buttonElement, {
            scale: 1.15,
            y: -10,
            boxShadow:
                "0 0 80px rgba(176, 214, 255, 0.8), 0 0 160px rgba(168, 213, 186, 0.6), 0 20px 40px rgba(0,0,0,0.3)",
            duration: 0.5,
            ease: "power2.out",
          })

          // Blur and darken all other sections
          const otherSections = document.querySelectorAll("section:not(.cta-section), nav, header")
          otherSections.forEach((section) => {
            gsap.to(section, {
              filter: "blur(6px) brightness(0.4)",
              duration: 0.6,
              ease: "power2.out",
            })
          })

          // Enhanced button internal effects
          const buttonGlow = buttonElement.querySelector(".button-glow")
          const sparkles = buttonElement.querySelector(".sparkles")
          const arrow = buttonElement.querySelector(".arrow")
          const pulseGlow = buttonElement.querySelector(".pulse-glow")

          if (buttonGlow) {
            gsap.to(buttonGlow, {
              opacity: 1,
              scale: 1.3,
              duration: 0.5,
              ease: "power2.out",
            })
          }

          if (sparkles) {
            gsap.to(sparkles, {
              rotation: 360,
              scale: 1.2,
              duration: 0.8,
              ease: "power2.out",
            })
          }

          if (arrow) {
            gsap.to(arrow, {
              x: 8,
              scale: 1.1,
              duration: 0.3,
              ease: "power2.out",
            })
          }

          // Pulsing glow animation
          if (pulseGlow) {
            gsap.to(pulseGlow, {
              opacity: 0.8,
              scale: 1.5,
              duration: 1,
              ease: "power2.inOut",
              yoyo: true,
              repeat: -1,
            })
          }
        })

        buttonElement.addEventListener("mouseleave", () => {
          // Reset button
          gsap.to(buttonElement, {
            scale: 1,
            y: 0,
            boxShadow: "0 0 30px rgba(176, 214, 255, 0.4), 0 10px 20px rgba(0,0,0,0.2)",
            duration: 0.5,
            ease: "power2.out",
          })

          // Remove blur and darken effect from other sections
          const otherSections = document.querySelectorAll("section:not(.cta-section), nav, header")
          otherSections.forEach((section) => {
            gsap.to(section, {
              filter: "blur(0px) brightness(1)",
              duration: 0.6,
              ease: "power2.out",
            })
          })

          // Reset button internal effects
          const buttonGlow = buttonElement.querySelector(".button-glow")
          const sparkles = buttonElement.querySelector(".sparkles")
          const arrow = buttonElement.querySelector(".arrow")
          const pulseGlow = buttonElement.querySelector(".pulse-glow")

          if (buttonGlow) {
            gsap.to(buttonGlow, {
              opacity: 0,
              scale: 1,
              duration: 0.5,
              ease: "power2.out",
            })
          }

          if (sparkles) {
            gsap.to(sparkles, {
              rotation: 0,
              scale: 1,
              duration: 0.8,
              ease: "power2.out",
            })
          }

          if (arrow) {
            gsap.to(arrow, {
              x: 0,
              scale: 1,
              duration: 0.3,
              ease: "power2.out",
            })
          }

          // Stop pulsing glow
          if (pulseGlow) {
            gsap.killTweensOf(pulseGlow)
            gsap.to(pulseGlow, {
              opacity: 0,
              scale: 1,
              duration: 0.3,
              ease: "power2.out",
            })
          }
        })

        // Magnetic effect
        buttonElement.addEventListener("mousemove", (e: MouseEvent) => {
          const rect = buttonElement.getBoundingClientRect()
          const centerX = rect.left + rect.width / 2
          const centerY = rect.top + rect.height / 2
          const deltaX = (e.clientX - centerX) * 0.15
          const deltaY = (e.clientY - centerY) * 0.15

          gsap.to(buttonElement, {
            x: deltaX,
            y: deltaY - 10, // Keep the -10 offset from hover
            duration: 0.3,
            ease: "power2.out",
          })
        })

        buttonElement.addEventListener("mouseleave", () => {
          gsap.to(buttonElement, {
            x: 0,
            y: 0,
            duration: 0.6,
            ease: "elastic.out(1, 0.3)",
          })
        })
      }

      // Animated background particles with proper scroll sync
      gsap.to(".bg-particle", {
        y: -100,
        opacity: 0.8,
        duration: 4,
        ease: "none",
        stagger: {
          each: 0.5,
          repeat: -1,
          yoyo: true,
        },
        scrollTrigger: {
          trigger: sectionRef.current,
          start: "top bottom",
          end: "bottom top",
          scrub: 1,
          invalidateOnRefresh: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

    // Use the shared auth context so the CTA can simulate login.
    // Clicking the CTA will call `login()` and switch the CTA to the
    // original Dashboard link. Replace `login()` with your real auth
    // flow later (e.g. open login modal, call API, then `login(user)`).
    const { loggedIn, login } = useAuth()

    return (
      <section ref={sectionRef} className="cta-section relative py-40 px-6 overflow-hidden">
        {/* Enhanced Animated Background */}
        <div ref={backgroundRef} className="absolute inset-0">
          {/* Gradient Base */}
          <div className="absolute inset-0 bg-gradient-to-t from-black via-gray-900 to-black" />

          {/* Animated Orbs */}
          <div className="absolute top-1/4 left-1/4 w-64 h-64 rounded-full bg-gradient-radial from-[#B0D6FF]/20 to-transparent blur-3xl animate-pulse" />
          <div
              className="absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full bg-gradient-radial from-[#A8D5BA]/15 to-transparent blur-3xl animate-pulse"
              style={{ animationDelay: "1s" }}
          />
          <div
              className="absolute top-1/2 left-1/2 w-96 h-96 rounded-full bg-gradient-radial from-[#FFD8A8]/10 to-transparent blur-3xl animate-pulse transform -translate-x-1/2 -translate-y-1/2"
              style={{ animationDelay: "2s" }}
          />

          {/* Floating Particles */}
          {Array.from({ length: 20 }).map((_, i) => (
              <div
                  key={i}
                  className="bg-particle absolute w-2 h-2 bg-white/20 rounded-full"
                  style={{
                    left: `${Math.random() * 100}%`,
                    top: `${Math.random() * 100}%`,
                    animationDelay: `${Math.random() * 4}s`,
                  }}
              />
          ))}
        </div>

        <div className="relative z-10 max-w-5xl mx-auto text-center">
          <div ref={contentRef}>
            <h2 className="text-6xl md:text-8xl font-bold mb-8 text-white">
                Experience Nature's Intelligence
            </h2>
            <p className="text-2xl text-white/80 mb-16 max-w-3xl mx-auto leading-relaxed">
                Discover how Orbis-Sentio transforms your space into a hub for climate science, biodiversity research, and community protection. Join the future of environmental monitoring.
            </p>
          </div>

          <div ref={buttonRef} className="flex justify-center">
            {!loggedIn ? (
              <Button
                  size="lg"
                  // TODO: replace this handler with a call to your real
                  // login flow. Example:
                  //  - navigate('/login') // route to login page
                  //  - or openLoginModal()
                  // After a successful login call `login(user)` from the
                  // auth provider to update the app state.
                  onClick={() => login()}
                  className="relative bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black font-bold px-20 py-10 text-3xl rounded-3xl group overflow-hidden shadow-2xl"
              >
                {/* Pulse Glow Effect */}
                <div className="pulse-glow absolute inset-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] rounded-3xl opacity-0 blur-2xl scale-110" />

                {/* Button Glow Effect */}
                <div className="button-glow absolute inset-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] rounded-3xl opacity-0 blur-xl scale-120" />

                {/* Button Content */}
                <div className="relative flex items-center justify-center">
                  <Sparkles className="sparkles w-8 h-8 mr-4" />
                  <span className="font-black tracking-wide text-2xl">Login</span>
                  <ArrowRight className="arrow w-8 h-8 ml-4" />
                </div>

                {/* Shimmer Effect */}
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent skew-x-12 -translate-x-full group-hover:translate-x-full transition-transform duration-1000" />

                {/* Border Glow */}
                <div className="absolute inset-0 rounded-3xl border-2 border-white/20 group-hover:border-white/40 transition-all duration-300" />
              </Button>
            ) : (
              <Link to="/dashboard">
                <Button
                    size="lg"
                    className="relative bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black font-bold px-20 py-10 text-3xl rounded-3xl group overflow-hidden shadow-2xl"
                >
                  {/* Pulse Glow Effect */}
                  <div className="pulse-glow absolute inset-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] rounded-3xl opacity-0 blur-2xl scale-110" />

                  {/* Button Glow Effect */}
                  <div className="button-glow absolute inset-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] rounded-3xl opacity-0 blur-xl scale-120" />

                  {/* Button Content */}
                  <div className="relative flex items-center justify-center">
                    <Sparkles className="sparkles w-8 h-8 mr-4" />
                    <span className="font-black tracking-wide text-2xl">Start Monitoring</span>
                    <ArrowRight className="arrow w-8 h-8 ml-4" />
                  </div>

                  {/* Shimmer Effect */}
                  <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent skew-x-12 -translate-x-full group-hover:translate-x-full transition-transform duration-1000" />

                  {/* Border Glow */}
                  <div className="absolute inset-0 rounded-3xl border-2 border-white/20 group-hover:border-white/40 transition-all duration-300" />
                </Button>
              </Link>
            )}
          </div>
        </div>
      </section>
  )
}
