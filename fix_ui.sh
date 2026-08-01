sed -i 's/val isDrawerVisible = isWideScreen && currentlyPlaying == null && !isAuthRequired/val isDrawerVisible = isWideScreen \&\& currentlyPlaying == null \&\& !isAuthRequired \&\& selectedTab == MainTab.HOME/g' app/src/main/java/com/example/MainActivity.kt
sed -i '12i import androidx.activity.compose.BackHandler' app/src/main/java/com/example/MainActivity.kt
