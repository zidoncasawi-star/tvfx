package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.NetflixRed

data class CountryFilter(
    val code: String,
    val nameResId: Int,
    val flag: String,
    val keywords: List<String>
)

val countriesList = listOf(
    CountryFilter(
        code = "all",
        nameResId = R.string.country_all,
        flag = "🌍",
        keywords = emptyList()
    ),
    CountryFilter(
        code = "ar",
        nameResId = R.string.country_ar,
        flag = "🇸🇦",
        keywords = listOf(
            "|ar|", "[ar]", "(ar)", "arabic", "morocco", "algeria", "tunisia", "egypt", "lebanon", "uae",
            "mauritania", "libya", "bahrain", "jordan", "yemen", "saoudi", "syria", "iraq", "sudan", "palestine",
            "qatar", "kuwait", "عربي", "عربية", "مصر", "المغرب", "تونس", "الجزائر", "السعودية", "خليجية", "عراقية",
            "سورية", "لبنانية", "شاهد", "رمضان", "مصرية", "برامج رمضان", "قرآن", "قران", "qur'an", "quran", "روتانا", "mbc", "osn", "الوان", "طرب", "shahid"
        )
    ),
    CountryFilter(
        code = "fr",
        nameResId = R.string.country_fr,
        flag = "🇫🇷",
        keywords = listOf(
            "|fr|", "[fr]", "(fr)", "france", "french", "|eu|", "[eu]", "disney+ [fr]", "netflix [fr]", "prime+ [fr]", "apple tv [fr]",
            "l'equipe", "ligue 1", "canal+", "meilleur des", "|be|", "[be]", "(be)", "belgique", "belgium"
        )
    ),
    CountryFilter(
        code = "us",
        nameResId = R.string.country_us,
        flag = "🇺🇸",
        keywords = listOf(
            "|us|", "[us]", "(us)", "|usa|", "[usa]", "|en|", "[en]", "|na|", "[na]", "|am|", "[am]", "english",
            "united states", "america", "usa", "hulu", "peacock", "paramount+", "hbo", "espn", "nba", "nfl", "nhl", "mlb", "24/7 english", "cricket",
            "|uk|", "[uk]", "(uk)", "united kingdom", "britain", "uk", "sky store", "discovery+",
            "canada", "canadian"
        )
    ),
    CountryFilter(
        code = "es",
        nameResId = R.string.country_es,
        flag = "🇪🇸",
        keywords = listOf(
            "|es|", "[es]", "(es)", "|latino|", "[latino]", "españa", "spanish", "spain", "latino", "mexico", "colombia", "argentina", "chile",
            "tivify", "deportes", "pelicuals", "niños", "saga [es]", "prime+ [es]"
        )
    ),
    CountryFilter(
        code = "de",
        nameResId = R.string.country_de,
        flag = "🇩🇪",
        keywords = listOf(
            "|de|", "[de]", "(de)", "germany", "deutschland", "deutsch",
            "|ch|", "[ch]", "(ch)", "swiss", "suisse", "|at|", "[at]", "(at)", "austria", "österreich"
        )
    ),
    CountryFilter(
        code = "it",
        nameResId = R.string.country_it,
        flag = "🇮🇹",
        keywords = listOf("|it|", "[it]", "(it)", "italy", "italia", "italian")
    ),
    CountryFilter(
        code = "tr",
        nameResId = R.string.country_tr,
        flag = "🇹🇷",
        keywords = listOf("|tr|", "[tr]", "(tr)", "turkey", "turkish", "تركي", "تركية")
    ),
    CountryFilter(
        code = "pl",
        nameResId = R.string.country_pl,
        flag = "🇵🇱",
        keywords = listOf("|pl|", "[pl]", "(pl)", "poland", "polish", "polskie", "bajki")
    ),
    CountryFilter(
        code = "pt",
        nameResId = R.string.country_pt,
        flag = "🇵🇹",
        keywords = listOf("|pt|", "[pt]", "(pt)", "portugal", "portugues", "brazil")
    ),
    CountryFilter(
        code = "nl",
        nameResId = R.string.country_nl,
        flag = "🇳🇱",
        keywords = listOf("|nl|", "[nl]", "(nl)", "netherlands", "nederland", "netherland")
    ),
    CountryFilter(
        code = "ru",
        nameResId = R.string.country_ru,
        flag = "🇷🇺",
        keywords = listOf("|ru|", "[ru]", "(ru)", "russia", "russian")
    ),
    CountryFilter(
        code = "in",
        nameResId = R.string.country_in,
        flag = "🇮🇳",
        keywords = listOf("|in|", "[in]", "(in)", "india", "indian", "tamil", "punjabi", "malayalam", "telugu", "kannada", "bhojpuri")
    ),
    CountryFilter(
        code = "pk",
        nameResId = R.string.country_pk,
        flag = "🇵🇰",
        keywords = listOf("|pk|", "[pk]", "(pk)", "pakistan")
    ),
    CountryFilter(
        code = "al",
        nameResId = R.string.country_al,
        flag = "🇦🇱",
        keywords = listOf("|al|", "[al]", "(al)", "albania")
    ),
    CountryFilter(
        code = "il",
        nameResId = R.string.country_il,
        flag = "🇮🇱",
        keywords = listOf("|il|", "[il]", "(il)", "israel")
    ),
    CountryFilter(
        code = "af",
        nameResId = R.string.country_af,
        flag = "🇿🇦",
        keywords = listOf("|af|", "[af]", "(af)", "africa", "african")
    ),
    CountryFilter(
        code = "gr",
        nameResId = R.string.country_gr,
        flag = "🇬🇷",
        keywords = listOf("|gr|", "[gr]", "(gr)", "greece", "greek")
    ),
    CountryFilter(
        code = "ro",
        nameResId = R.string.country_ro,
        flag = "🇷🇴",
        keywords = listOf("|ro|", "[ro]", "(ro)", "romania")
    ),
    CountryFilter(
        code = "sc",
        nameResId = R.string.country_sc,
        flag = "🇸🇪",
        keywords = listOf(
            "scandinavia", "scandinavian",
            "|se|", "[se]", "(se)", "sweden", "sverige",
            "|no|", "[no]", "(no)", "norway", "norge",
            "|dk|", "[dk]", "(dk)", "denmark", "danmark",
            "|is|", "[is]", "(is)", "iceland"
        )
    ),
    CountryFilter(
        code = "so",
        nameResId = R.string.country_so,
        flag = "🇸🇴",
        keywords = listOf("|so|", "[so]", "(so)", "somalia")
    ),
    CountryFilter(
        code = "bg",
        nameResId = R.string.country_bg,
        flag = "🇧🇬",
        keywords = listOf("|bg|", "[bg]", "(bg)", "bulgaria")
    ),
    CountryFilter(
        code = "exyu",
        nameResId = R.string.country_exyu,
        flag = "🇪🇺",
        keywords = listOf("exyu", "ex-yu", "yugoslavia")
    ),
    CountryFilter(
        code = "fi",
        nameResId = R.string.country_fi,
        flag = "🇫🇮",
        keywords = listOf("|fi|", "[fi]", "(fi)", "finland", "suomi")
    ),
    CountryFilter(
        code = "hu",
        nameResId = R.string.country_hu,
        flag = "🇭🇺",
        keywords = listOf("|hu|", "[hu]", "(hu)", "hungary", "hungaria")
    ),
    CountryFilter(
        code = "ir",
        nameResId = R.string.country_ir,
        flag = "🇮🇷",
        keywords = listOf("|ir|", "[ir]", "(ir)", "iran", "persian")
    ),
    CountryFilter(
        code = "netflix",
        nameResId = R.string.country_netflix,
        flag = "🍿",
        keywords = listOf("netflix")
    ),
    CountryFilter(
        code = "apple",
        nameResId = R.string.country_apple,
        flag = "🍏",
        keywords = listOf("apple tv", "apple+")
    ),
    CountryFilter(
        code = "disney",
        nameResId = R.string.country_disney,
        flag = "🏰",
        keywords = listOf("disney", "disney+")
    ),
    CountryFilter(
        code = "adult",
        nameResId = R.string.country_adult,
        flag = "🔞",
        keywords = listOf("adult", "+18", "18+")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryFlagsFilterBar(
    selectedCountryCode: String,
    onCountrySelect: (String) -> Unit,
    availableCountries: List<CountryFilter>,
    modifier: Modifier = Modifier
) {
    if (availableCountries.size <= 1) {
        // If only "all" is available, don't show the filter bar to avoid taking up empty space
        return
    }

    val context = LocalContext.current

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = "تصفية حسب اللغة",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(availableCountries) { country ->
                val isSelected = country.code == selectedCountryCode
                val countryName = stringResource(id = country.nameResId)
                
                // Dynamically check if a flag icon exists in the drawable resources (the "software folder")
                val flagResId = remember(country.code) {
                    context.resources.getIdentifier("ic_flag_${country.code}", "drawable", context.packageName)
                }
                
                Surface(
                    onClick = { onCountrySelect(country.code) },
                    shape = CircleShape,
                    color = if (isSelected) NetflixRed else DarkCardBg,
                    border = if (isSelected) {
                        null
                    } else {
                        CardDefaults.outlinedCardBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.08f))
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (flagResId != 0) {
                            // Display the flag ONLY if an icon is available in the software folder
                            Image(
                                painter = painterResource(id = flagResId),
                                contentDescription = countryName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            // No icon available in the software folder - don't show a flag, show country code text fallback
                            Text(
                                text = country.code.uppercase().take(2),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
