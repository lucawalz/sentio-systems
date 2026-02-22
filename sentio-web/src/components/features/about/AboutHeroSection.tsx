"use client"

import { motion } from "framer-motion"
import { Button } from "@/components/ui/button"
import { Link } from "react-router-dom"

const stats = [
    { label: "Sensor Types", value: "15+" },
    { label: "Species Detected", value: "10K+" },
    { label: "Countries", value: "30+" },
    { label: "Uptime", value: "99.9%" },
]

const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: {
            staggerChildren: 0.1,
        },
    },
}

const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: {
        opacity: 1,
        y: 0,
        transition: { duration: 0.5, ease: "easeOut" as const },
    },
}

export default function AboutHeroSection() {
    return (
        <section className="bg-background py-16 md:py-32">
            <div className="mx-auto max-w-5xl border-t px-6">
                <span className="text-caption -ml-6 -mt-3.5 block w-max bg-background px-6">About</span>

                <motion.div
                    className="mt-12 gap-4 sm:grid sm:grid-cols-2 md:mt-24"
                    initial="hidden"
                    animate="visible"
                    variants={containerVariants}
                >
                    <motion.div className="sm:w-4/5" variants={itemVariants}>
                        <h1 className="text-3xl font-bold sm:text-4xl lg:text-5xl">
                            Building global environmental intelligence
                        </h1>
                    </motion.div>
                    <motion.div className="mt-6 sm:mt-0" variants={itemVariants}>
                        <p>
                            We're building a distributed network that connects local sensors
                            with powerful AI to enable better climate models and protect
                            biodiversity worldwide.
                        </p>
                        <div className="mt-6">
                            <Button asChild>
                                <Link to="/contact">Get in touch</Link>
                            </Button>
                        </div>
                    </motion.div>
                </motion.div>

                <motion.div
                    className="mt-12 grid grid-cols-2 gap-6 md:mt-24 md:grid-cols-4"
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={containerVariants}
                >
                    {stats.map((stat) => (
                        <motion.div
                            key={stat.label}
                            className="space-y-2"
                            variants={itemVariants}
                        >
                            <p className="text-3xl font-bold md:text-4xl">{stat.value}</p>
                            <p className="text-muted-foreground text-sm">{stat.label}</p>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    )
}
