import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent } from '@/components/ui/card'

type Testimonial = {
    name: string
    role: string
    image: string
    quote: string
}

const testimonials: Testimonial[] = [
    {
        name: 'Dr. Elena Vasquez',
        role: 'Marine Biologist',
        image: 'https://randomuser.me/api/portraits/women/1.jpg',
        quote: 'Game-changer for coastal monitoring.',
    },
    {
        name: 'Thomas Bergström',
        role: 'Forest Ranger',
        image: 'https://randomuser.me/api/portraits/men/6.jpg',
        quote: 'The species recognition identified 47 bird species in our reserve that we had no idea were there. The acoustic sensors pick up calls we could never detect manually.',
    },
    {
        name: 'Dr. Amara Osei',
        role: 'Climate Scientist',
        image: 'https://randomuser.me/api/portraits/women/7.jpg',
        quote: 'Finally, hyperlocal climate data we can actually trust.',
    },
    {
        name: 'Miguel Santos',
        role: 'Organic Farmer',
        image: 'https://randomuser.me/api/portraits/men/8.jpg',
        quote: 'I was skeptical about another "smart farming" solution, but Sentio is different. The frost warnings have saved my crops twice this season. The soil moisture tracking means I water exactly when needed - saving water and getting better yields. Setup took 20 minutes. Worth every penny.',
    },
    {
        name: 'Prof. Yuki Tanaka',
        role: 'Environmental Studies',
        image: 'https://randomuser.me/api/portraits/women/4.jpg',
        quote: 'We use Sentio in our research program. Students can deploy sensors and see real data within hours. The contribution to global climate models gives their work real meaning.',
    },
    {
        name: 'David Okonkwo',
        role: 'Wildlife Photographer',
        image: 'https://randomuser.me/api/portraits/men/2.jpg',
        quote: 'The biodiversity alerts tell me exactly when and where animals are active. My hit rate for wildlife shots has tripled.',
    },
    {
        name: 'Dr. Ingrid Larsen',
        role: 'Glaciologist',
        image: 'https://randomuser.me/api/portraits/women/5.jpg',
        quote: 'Deployed 12 nodes across our glacier study site. Temperature accuracy is within 0.1°C. The battery life in extreme cold exceeded expectations.',
    },
    {
        name: 'Carlos Rivera',
        role: 'Urban Planner',
        image: 'https://randomuser.me/api/portraits/men/9.jpg',
        quote: 'We mapped urban heat islands across the city using the sensor network. The data directly influenced our green infrastructure plan. City council approved the budget based on our findings.',
    },
    {
        name: 'Fatima Al-Hassan',
        role: 'Agricultural Consultant',
        image: 'https://randomuser.me/api/portraits/women/10.jpg',
        quote: 'Accurate. Reliable. Easy to use.',
    },
    {
        name: 'Dr. James Chen',
        role: 'Atmospheric Researcher',
        image: 'https://randomuser.me/api/portraits/men/11.jpg',
        quote: 'The air quality sensors catch pollution events that our regional monitors miss completely. Hyperlocal data fills critical gaps in our models.',
    },
    {
        name: 'Sophie Andersson',
        role: 'Beekeeper',
        image: 'https://randomuser.me/api/portraits/women/12.jpg',
        quote: 'I track temperature and humidity in all my hives. The alerts when conditions change help me keep my colonies healthy.',
    },
    {
        name: 'Dr. Kofi Mensah',
        role: 'Ecologist',
        image: 'https://randomuser.me/api/portraits/men/13.jpg',
        quote: 'Outstanding platform. The API is well-documented, the data export is seamless, and the research network means our small study contributes to something much larger.',
    },
]

const chunkArray = (array: Testimonial[], chunkSize: number): Testimonial[][] => {
    const result: Testimonial[][] = []
    for (let i = 0; i < array.length; i += chunkSize) {
        result.push(array.slice(i, i + chunkSize))
    }
    return result
}

const testimonialChunks = chunkArray(testimonials, Math.ceil(testimonials.length / 3))

export default function WallOfLoveSection() {
    return (
        <section>
            <div className="py-16 md:py-32">
                <div className="mx-auto max-w-6xl px-6">
                    <div className="text-center">
                        <h2 className="text-3xl font-semibold">What our users are saying</h2>
                        <p className="mt-6">From researchers to farmers, see how Sentio is making a difference.</p>
                    </div>
                    <div className="mt-8 grid gap-3 sm:grid-cols-2 md:mt-12 lg:grid-cols-3">
                        {testimonialChunks.map((chunk, chunkIndex) => (
                            <div
                                key={chunkIndex}
                                className="space-y-3">
                                {chunk.map(({ name, role, quote, image }, index) => (
                                    <Card key={index}>
                                        <CardContent className="grid grid-cols-[auto_1fr] gap-3 pt-6">
                                            <Avatar className="size-9">
                                                <AvatarImage
                                                    alt={name}
                                                    src={image}
                                                    loading="lazy"
                                                    width="120"
                                                    height="120"
                                                />
                                                <AvatarFallback>ST</AvatarFallback>
                                            </Avatar>

                                            <div>
                                                <h3 className="font-medium">{name}</h3>

                                                <span className="text-muted-foreground block text-sm tracking-wide">{role}</span>

                                                <blockquote className="mt-3">
                                                    <p className="text-gray-700 dark:text-gray-300">{quote}</p>
                                                </blockquote>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </section>
    )
}
