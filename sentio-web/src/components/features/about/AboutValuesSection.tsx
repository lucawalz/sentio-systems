"use client"

import { motion } from "framer-motion"
import { Globe, HeartHandshake, ShieldCheck, Sparkles } from "lucide-react"

const values = [
    {
        title: "Open Data",
        description: "We believe environmental data should be accessible. Contributing to open research benefits everyone.",
        icon: HeartHandshake,
    },
    {
        title: "Privacy-First",
        description: "Your data stays yours. End-to-end encryption and local processing protect your information.",
        icon: ShieldCheck,
    },
    {
        title: "Scientific Rigor",
        description: "Every algorithm is validated against peer-reviewed research. Accuracy matters.",
        icon: Sparkles,
    },
    {
        title: "Global Impact",
        description: "Building a worldwide network of sensors to improve climate predictions for everyone.",
        icon: Globe,
    },
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

export default function AboutValuesSection() {
    return (
        <section className="bg-background py-16 md:py-32">
            <div className="mx-auto max-w-5xl border-t px-6">
                <span className="text-caption -ml-6 -mt-3.5 block w-max bg-background px-6">Values</span>

                <motion.div
                    className="mt-12 gap-4 sm:grid sm:grid-cols-2 md:mt-24"
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={containerVariants}
                >
                    <motion.div className="sm:w-4/5" variants={itemVariants}>
                        <h2 className="text-3xl font-bold sm:text-4xl">
                            Principles that guide us
                        </h2>
                    </motion.div>
                    <motion.div className="mt-6 sm:mt-0" variants={itemVariants}>
                        <p>
                            Building environmental intelligence that's accurate, private,
                            and contributes to global climate research.
                        </p>
                    </motion.div>
                </motion.div>

                <motion.div
                    className="mt-12 grid gap-8 sm:grid-cols-2 md:mt-24"
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={containerVariants}
                >
                    {values.map((value) => (
                        <motion.div
                            key={value.title}
                            className="group"
                            variants={itemVariants}
                        >
                            <div className="flex items-start gap-4">
                                <div className="text-primary">
                                    <value.icon className="h-6 w-6" />
                                </div>
                                <div>
                                    <h3 className="text-lg font-medium">{value.title}</h3>
                                    <p className="text-muted-foreground mt-2 text-sm">
                                        {value.description}
                                    </p>
                                </div>
                            </div>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    )
}
