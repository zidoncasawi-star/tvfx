sed -i 's/\.androidx\.compose\.ui\.zIndex\.zIndex(1f)/\.zIndex(1f)/g' app/src/main/java/com/example/MainActivity.kt
sed -i '11i import androidx.compose.ui.zIndex.zIndex' app/src/main/java/com/example/MainActivity.kt
sed -i '12i import androidx.compose.material.icons.filled.PlayArrow' app/src/main/java/com/example/ui/screens/LiveTvScreen.kt
sed -i 's/androidx\.compose\.material\.icons\.Icons\.Filled\.PlayArrow/Icons.Default.PlayArrow/g' app/src/main/java/com/example/ui/screens/LiveTvScreen.kt
