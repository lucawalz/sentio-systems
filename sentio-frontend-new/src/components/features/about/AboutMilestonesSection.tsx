"use client"

import { motion } from "framer-motion"
import { InteractiveTimeline } from "@/components/ui/interactive-timeline"

const milestones = [
    {
        id: "1",
        title: "Vision",
        description: "Identified the need for hyperlocal environmental data to improve climate models and predictions.",
        date: "2023",
    },
    {
        id: "2",
        title: "Orbis Prototype",
        description: "Developed the first hardware prototypes: Hub, Nodes, and initial sensor integrations.",
        date: "2024 Q1",
    },
    {
        id: "3",
        title: "Sentio Platform",
        description: "Built the cloud and AI platform for data processing, species recognition, and climate analysis.",
        date: "2024 Q2",
    },
    {
        id: "4",
        title: "Integration",
        description: "Connected the complete data flow from sensors through AI analysis to the user dashboard.",
        date: "2024 Q3",
    },
    {
        id: "5",
        title: "Global Network",
        description: "Launching the distributed environmental intelligence network for worldwide climate research.",
        date: "2025",
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

export default function AboutMilestonesSection() {
    return (
        <section className="bg-background py-16 md:py-32">
            <div className="mx-auto max-w-5xl border-t px-6">
                <span className="text-caption -ml-6 -mt-3.5 block w-max bg-background px-6">Journey</span>

                <motion.div
                    className="mt-12 gap-4 sm:grid sm:grid-cols-2 md:mt-24"
                    initial="hidden"
                    whileInView="visible"
                    viewport={{ once: true }}
                    variants={containerVariants}
                >
                    <motion.div className="sm:w-4/5" variants={itemVariants}>
                        <h2 className="text-3xl font-bold sm:text-4xl">
                            Our journey so far
                        </h2>
                    </motion.div>
                    <motion.div className="mt-6 sm:mt-0" variants={itemVariants}>
                        <p>
                            From concept to functional prototype, building the foundation
                            for global environmental intelligence.
                        </p>
                    </motion.div>
                </motion.div>

                <div className="mt-12 md:mt-24">
                    <InteractiveTimeline items={milestones} />
                </div>
            </div>
        </section>
    )
}
