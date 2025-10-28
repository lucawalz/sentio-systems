import type React from "react"

import { useEffect, useState, useRef } from "react"
import { gsap } from "gsap"
import { Button } from "../ui/button"
import { Link } from "react-router-dom"
import { Menu, X } from "lucide-react"

export function Navigation() {
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const navRef = useRef<HTMLElement>(null)
  const logoRef = useRef<HTMLDivElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleScroll = () => {
      const scrolled = window.scrollY > 50
      setIsScrolled(scrolled)
    }

    window.addEventListener("scroll", handleScroll)

    // Enhanced nav animations
    gsap.fromTo(
      navRef.current,
      { y: -100, opacity: 0 },
      { y: 0, opacity: 1, duration: 1, ease: "power3.out", delay: 0.5 },
    )

    gsap.fromTo(
      logoRef.current,
      { scale: 0, rotation: -180 },
      { scale: 1, rotation: 0, duration: 1.2, ease: "back.out(1.7)", delay: 0.8 },
    )

    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  return (
    <nav
      ref={navRef}
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-700 ${
        isScrolled ? "bg-black/90 backdrop-blur-2xl border-b border-white/20 shadow-2xl" : "bg-transparent"
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
        <div ref={logoRef} className="text-2xl font-bold tracking-tight cursor-pointer group">
          <span className="bg-gradient-to-r from-[#B0D6FF] via-white to-[#A8D5BA] bg-clip-text text-transparent">
            Sentio
          </span>
          <div className="h-0.5 w-0 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-500 group-hover:w-full" />
        </div>

        {/* Desktop Menu */}
        <div ref={menuRef} className="hidden md:flex items-center space-x-8">
          {["Our Story", "How It Works", "Features"].map((item, index) => (
            <a
              key={item}
              href={`#${item.toLowerCase().replace(/\s+/g, "-")}`}
              className="text-white/70 hover:text-white transition-all duration-300 relative group preload-fade"
              style={{ "--delay": `${index * 0.1}s` } as React.CSSProperties}
            >
              {item}
              <div className="absolute -bottom-1 left-0 w-0 h-0.5 bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] transition-all duration-300 group-hover:w-full" />
            </a>
          ))}
          <Link to="/dashboard">
            <Button className="bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black hover:shadow-2xl hover:shadow-blue-500/25 font-medium transition-all duration-300 preload-fade">
              View Dashboard
            </Button>
          </Link>
        </div>

        {/* Mobile Menu Button */}
        <button className="md:hidden text-white" onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}>
          {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Mobile Menu */}
      {isMobileMenuOpen && (
        <div className="md:hidden bg-black/95 backdrop-blur-2xl border-b border-white/20">
          <div className="px-6 py-4 space-y-4">
            {["Our Story", "How It Works", "Features"].map((item) => (
              <a
                key={item}
                href={`#${item.toLowerCase().replace(/\s+/g, "-")}`}
                className="block text-white/70 hover:text-white transition-colors"
                onClick={() => setIsMobileMenuOpen(false)}
              >
                {item}
              </a>
            ))}
            <Link to="/dashboard">
              <Button className="w-full bg-gradient-to-r from-[#B0D6FF] to-[#A8D5BA] text-black font-medium">
                View Dashboard
              </Button>
            </Link>
          </div>
        </div>
      )}
    </nav>
  )
}
