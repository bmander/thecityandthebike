package com.thecityandthebike.data.model

object ScoreRules {
    data class Rule(val id: String, val label: String, val points: Int)

    val photoRules = listOf(
        Rule("add_image", "Took a photo", 2),
        Rule("first_bike_today", "First photo of this bike today", 5),
        Rule("first_bike_for_user", "Your first photo of this bike", 5),
        Rule("first_bike_ever", "First photo of this bike ever", 10),
    )

    val tagRules = listOf(
        Rule("tag_1", "1st tag on a submission", 2),
        Rule("tag_2", "2nd tag", 1),
        Rule("tag_3", "3rd tag", 1),
        Rule("tag_4_plus", "4th+ tags", 0),
    )
}
