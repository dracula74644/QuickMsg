package com.quickmsg.app.data

data class QuickMessage(
    val text: String,
    val emoji: String,
    val category: String
)

object QuickMessages {
    val categories = listOf(
        "Status", "Quick Reply", "Busy", "Love", "Food", "Templates"
    )

    val all: List<QuickMessage> = listOf(
        QuickMessage("On my way!", "🚗", "Status"),
        QuickMessage("Reached safely", "✅", "Status"),
        QuickMessage("Leaving now", "👋", "Status"),
        QuickMessage("Running late", "⏰", "Status"),
        QuickMessage("Stuck in traffic", "🚦", "Status"),
        QuickMessage("Almost there", "📍", "Status"),

        QuickMessage("OK", "👍", "Quick Reply"),
        QuickMessage("Got it", "✓", "Quick Reply"),
        QuickMessage("Will do!", "", "Quick Reply"),
        QuickMessage("Sure!", "", "Quick Reply"),
        QuickMessage("Thanks!", "❤️", "Quick Reply"),
        QuickMessage("Miss you", "💕", "Quick Reply"),

        QuickMessage("In a meeting", "🤫", "Busy"),
        QuickMessage("Can't talk now", "", "Busy"),
        QuickMessage("Call me in 10 mins", "", "Busy"),
        QuickMessage("Working late today", "", "Busy"),
        QuickMessage("Battery low", "🪫", "Busy"),

        QuickMessage("Thinking of you", "💭", "Love"),
        QuickMessage("Have a great day!", "☀️", "Love"),
        QuickMessage("Good morning", "❤️", "Love"),
        QuickMessage("Good night", "🌙", "Love"),
        QuickMessage("Love you", "💗", "Love"),
        QuickMessage("Take care!", "🤗", "Love"),

        QuickMessage("Lunch?", "🍽️", "Food"),
        QuickMessage("What should we eat?", "", "Food"),
        QuickMessage("I'll cook today", "👨‍🍳", "Food"),
        QuickMessage("Order food?", "🛵", "Food"),
        QuickMessage("Tea/Coffee?", "☕", "Food"),

        QuickMessage("ETA: {time_plus_15}", "🕐", "Templates"),
        QuickMessage("Coming by {time_plus_30}", "🕐", "Templates"),
        QuickMessage("Free after {time_plus_60}", "🕐", "Templates")
    )

    fun byCategory(category: String): List<QuickMessage> =
        all.filter { it.category == category }
}
