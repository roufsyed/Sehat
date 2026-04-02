package com.rouf.saht.common.model

data class MeditationQuote(val quote: String, val author: String)

object MeditationQuotes {
    val quotes: List<MeditationQuote> = listOf(
        MeditationQuote("The thing about meditation is: You become more and more you.", "David Lynch"),
        MeditationQuote("Quiet the mind, and the soul will speak.", "Ma Jaya Sati Bhagavati"),
        MeditationQuote("Meditation is the secret of all growth in spiritual life and knowledge.", "James Allen"),
        MeditationQuote("Your calm mind is the ultimate weapon against your challenges.", "Bryant McGill"),
        MeditationQuote("The mind is definitely something that can be transformed, and meditation is a means to transform it.", "Dalai Lama"),
        MeditationQuote("Within you, there is a stillness and a sanctuary to which you can retreat at any time.", "Hermann Hesse"),
        MeditationQuote("To understand the immeasurable, the mind must be extraordinarily quiet, still.", "Jiddu Krishnamurti"),
        MeditationQuote("Meditation is not about stopping thoughts, but recognizing that we are more than our thoughts and feelings.", "Arianna Huffington"),
        MeditationQuote("Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor.", "Thich Nhat Hanh"),
        MeditationQuote("Peace comes from within. Do not seek it without.", "Buddha"),
        MeditationQuote("Silence is not an absence but a presence.", "Anne D. LeClaire"),
        MeditationQuote("Meditation is the discovery that the point of life is always arrived at in the immediate moment.", "Alan Watts"),
        MeditationQuote("Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment.", "Buddha"),
        MeditationQuote("When meditation is mastered, the mind is unwavering like the flame of a candle in a windless place.", "Bhagavad Gita"),
        MeditationQuote("The quieter you become, the more you are able to hear.", "Rumi"),
        MeditationQuote("Be here now.", "Ram Dass"),
        MeditationQuote("Your goal is not to battle with the mind, but to witness the mind.", "Swami Muktananda"),
        MeditationQuote("Surrender to what is. Let go of what was. Have faith in what will be.", "Sonia Ricotti"),
        MeditationQuote("Meditation is a vital way to purify and quiet the mind, thus rejuvenating the body.", "Deepak Chopra"),
        MeditationQuote("If you want to conquer the anxiety of life, live in the moment, live in the breath.", "Amit Ray"),

        // Bhagavad Gita Quotes on Meditation
        MeditationQuote("A person who is not disturbed by happiness and distress and is steady in both is certainly eligible for liberation.", "Bhagavad Gita"),
        MeditationQuote("Set thy heart upon thy work, but never on its reward.", "Bhagavad Gita"),
        MeditationQuote("One who sees inaction in action, and action in inaction, is intelligent among men.", "Bhagavad Gita"),
        MeditationQuote("For one who has conquered the mind, the mind is the best of friends; but for one who has failed to do so, his mind will remain the greatest enemy.", "Bhagavad Gita"),
        MeditationQuote("Reshape yourself through the power of your will; never let yourself be degraded by self-will.", "Bhagavad Gita"),
        MeditationQuote("When meditation is mastered, the mind is unwavering like the flame of a lamp in a windless place.", "Bhagavad Gita"),
        MeditationQuote("The truly wise, resting in meditation, see beyond duality, beyond opposites, beyond attachments.", "Bhagavad Gita"),
        MeditationQuote("Calmness, gentleness, silence, self-restraint, and purity: these are the disciplines of the mind.", "Bhagavad Gita"),
        MeditationQuote("There is neither this world nor the world beyond nor happiness for the one who doubts.", "Bhagavad Gita"),
        MeditationQuote("Perform your obligatory duty, because action is indeed better than inaction.", "Bhagavad Gita")
    )

    fun getRandomQuote(): MeditationQuote {
        return quotes.random()
    }
}
