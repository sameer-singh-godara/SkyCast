## Overview of SkyCast

SkyCast is an Android weather application built with **Jetpack Compose** for the UI and **Kotlin** for the logic. It uses the **MVVM (Model-View-ViewModel)** architecture to manage data and UI state. The app fetches weather data based on the user's location or a searched city, supports customization (theme, font size, language), optimizes battery usage, includes a fun feature, and ensures accessibility with semantic properties.

The implementation leverages modern Android development practices, including **Coroutines** for asynchronous operations, **Google Play Services** for location, and **Navigation Compose** for screen management. Below is a detailed breakdown of how each feature and component is implemented.

---

## 1. Architecture and Core Components

### MVVM Architecture
- **Model**: Represents data classes like `MyLatLng` (for latitude and longitude) and weather/forecast response objects (`weatherResponse`, `forecastResponse`) stored in the `model/` package.
- **View**: Composable UI components (e.g., `WeatherSection`, `ForecastSection`, `LanguageSelectionDialog`) in the `view/` package, built with Jetpack Compose.
- **ViewModel**: `MainViewModel` (in `viewmodel/`) manages app state, handles weather API calls, and stores user preferences (e.g., dark mode, font size, language).

The `MainViewModel` is initialized in `MainActivity` using `ViewModelProvider` and serves as the single source of truth for weather data, UI state (`STATE.LOADING`, `STATE.SUCCESS`, `STATE.FAILED`), and settings.

### MainActivity
- **Role**: The entry point of the app, responsible for initializing location services, setting up navigation, and rendering the UI with Jetpack Compose.
- **Key Responsibilities**:
  - Initializes `FusedLocationProviderClient` for location updates.
  - Applies saved locale settings using `LanguageUtils.applySavedLocale`.
  - Sets up the Compose UI with `SkyCastTheme` and `AppNavigation`.
  - Manages location permission requests and updates.

### Navigation
- **Implementation**: Uses **Navigation Compose** with a `NavHost` and `NavController` to manage three screens: **Home**, **Search**, and **Settings**.
- **Screens**:
  - Defined as sealed class `Screen` with routes (`home`, `search`, `settings`).
  - Navigation is handled via a `Scaffold` with a `NavigationBar` containing three `NavigationBarItem` components, each tied to an icon and label (Home, Search, Settings).
- **Behavior**:
  - The `NavController` ensures single-top navigation with state restoration.
  - Invalid routes redirect to the Home screen using `LaunchedEffect` to monitor `currentRoute`.

### Theme and UI
- **SkyCastTheme**: A custom Compose theme (in `ui/theme/`) that supports:
  - **Dark/Light Mode**: Toggled via `MainViewModel.darkMode`.
  - **Font Scaling**: Adjusted via `MainViewModel.fontSizeScale` using `CompositionLocalProvider` and `LocalFontScale`.
- **System UI**: Uses `Accompanist SystemUiController` to manage system bar visibility.
- **Accessibility**: All UI components include `semantics` modifiers with `contentDescription` for screen reader support (e.g., navigation icons, buttons, text fields).

---

## 2. Feature Implementation Details

### Real-Time Weather
- **How It Works**:
  - The app fetches the device's location using `FusedLocationProviderClient` and requests weather data for that location.
  - Weather data is retrieved via `MainViewModel.getWeatherByLocation`, which likely calls a weather API (e.g., OpenWeatherMap, though the API call logic is not shown in the provided code).
- **Location Handling**:
  - **Initialization**: `initLocationClient` sets up `FusedLocationProviderClient` in `MainActivity`.
  - **Permission Check**: The app checks for location permissions (defined in `Const.Companion.permissions`) using `ContextCompat.checkSelfPermission`.
  - **Location Updates**:
    - `startLocationUpdate` requests high-accuracy location updates with a `LocationRequest` (interval: 100ms, min update: 3s, max delay: 100ms).
    - `fetchLocationWithTimeout` uses a coroutine with a 5-second timeout to retrieve the latest location (`MyLatLng`) and handles retries if the location is invalid (`lat == 0.0` or `lng == 0.0`).
  - **UI Updates**:
    - The `LocationScreen` (Home screen) displays the weather using `WeatherSection` when `MainViewModel.state == STATE.SUCCESS` and a valid location is available.
    - If location services are disabled, an error is shown via `ErrorSection`.

### Weather Forecast
- **How It Works**:
  - The app fetches a 5-day forecast using `MainViewModel.getForecastByLocation` for the current location or `searchForecastResponse` for searched cities.
  - The forecast is displayed in the `ForecastSection` composable when data is available.
- **Implementation**:
  - Forecast data is stored in `MainViewModel.forecastResponse` (for location-based) or `searchForecastResponse` (for city search).
  - The `LocationScreen` and `SearchScreen` conditionally render `ForecastSection` if the response list is non-empty.
  - The UI updates automatically when `MainViewModel.state` or `searchState` changes, triggered by `LaunchedEffect`.

### City Search
- **How It Works**:
  - Users can enter a city name in the `SearchScreen` to fetch weather and forecast data for that city.
- **Implementation**:
  - **UI**: An `OutlinedTextField` captures the city name (`locationQuery`), and a `Button` triggers the search.
  - **Logic**:
    - The search button calls `MainViewModel.getWeatherByLocationName`, which updates `searchWeatherResponse`, `searchForecastResponse`, and `searchLocationName`.
    - The `searchState` (`STATE.LOADING`, `STATE.SUCCESS`, `STATE.FAILED`) controls the UI:
      - `STATE.LOADING`: Shows `LoadingSection` with a `CircularProgressIndicator`.
      - `STATE.SUCCESS`: Displays `WeatherSection` and `ForecastSection` for the searched city.
      - `STATE.FAILED`: Shows `ErrorSection` with `searchErrorMessage` and a `Toast`.
  - **State Management**: `LaunchedEffect` monitors `searchState` to update `isLoading` and `showResults` flags, ensuring smooth UI transitions.

### Customizable Settings
The **SettingsScreen** allows users to customize the app's appearance and behavior.

#### Dark/Light Mode
- **Implementation**:
  - A `Switch` toggles `MainViewModel.darkMode`, which updates the `SkyCastTheme` (darkTheme parameter).
  - The UI recomposes automatically via `LaunchedEffect` when `darkMode` changes.
  - The setting is displayed in a `Card` with a `Row` containing the mode label (`Dark Mode` or `Light Mode`) and the switch.
- **Accessibility**: The switch has a `contentDescription` that indicates the action (e.g., "Toggle to Light Mode").

#### Font Size
- **Implementation**:
  - Users can adjust font size using two buttons: `Decrease Font Size` and `Increase Font Size`.
  - `MainViewModel.fontSizeScale` (a float between 0.5 and 1.5) controls the font scale, applied via `CompositionLocalProvider(LocalFontScale)`.
  - The current font size is displayed as `Small` (<=0.8), `Medium` (<=1.0), or `Large` (>1.0) in a `Text` composable.
  - Buttons are disabled when the scale reaches the minimum (0.5) or maximum (1.5).
- **UI**: Presented in a `Card` with a `Row` for buttons and the size label.
- **Accessibility**: Each button and label includes `contentDescription` for screen readers.

#### Language Selection
- **Implementation**:
  - A `LanguageSelectionDialog` (custom composable) allows users to select a language.
  - On selection, `MainViewModel.setLanguage` updates the language preference, and the app restarts (`startActivity` with `FLAG_ACTIVITY_CLEAR_TOP` and `FLAG_ACTIVITY_NEW_TASK`) to apply the new locale using `LanguageUtils.applySavedLocale`.
  - The saved language is retrieved via `LanguageUtils.getSavedLanguage` on app start.
- **UI**: Displayed in a `Card` with an underlined title and the dialog trigger.
- **Accessibility**: The dialog and title include semantic properties.

### Battery Optimization
- **How It Works**:
  - The app adjusts the frequency of location updates based on the device's battery level to conserve power.
- **Implementation**:
  - **Battery Monitoring**: `BatteryUtils.observeBatteryLevel` retrieves the current battery percentage.
  - **Refresh Intervals**:
    - >60% battery: Updates every 30 seconds (`refreshInterval = 30_000L`).
    - 30–60% battery: Updates every 2 minutes (`refreshInterval = 120_000L`).
    - <30% battery: Disables auto-refresh; shows a manual `Refresh` button on the Home screen.
  - **Logic**:
    - `LocationScreen` uses `LaunchedEffect` to check `isLocationEnabled`, `batteryPercentage`, and `allPermissionsGranted`.
    - If auto-refresh is enabled, it runs a loop to fetch location and weather data at the specified interval, using `fetchLocationWithTimeout` and `fetchWeatherInformation`.
    - The manual refresh button (shown when `shouldAutoRefresh` is false) triggers a coroutine to fetch location and weather data, with retries if the location is invalid.
  - **UI Feedback**: `Toast` messages inform users of retry attempts or permission issues.

### Fun Feature
- **How It Works**:
  - A "Fun Feature" is accessible via a button in the Settings screen, launching a separate `FunActivity`.
- **Implementation**:
  - The `SettingsScreen` includes a `Card` with a `Button` labeled "Let's Have Fun".
  - On click, an `Intent` starts `FunActivity`, passing the current `darkMode` state as an extra.
  - The implementation of `FunActivity` is not provided, but it likely offers an interactive or entertaining feature (e.g., a game or animation).
- **Accessibility**: The button includes a `contentDescription` for screen readers.

### Accessibility
- **How It Works**:
  - The app ensures compatibility with screen readers by adding semantic properties to all UI components.
- **Implementation**:
  - **Semantics Modifiers**: Every composable (e.g., `Button`, `Icon`, `Text`, `NavigationBar`, `Scaffold`) includes a `Modifier.semantics` with a `contentDescription`.
  - **Examples**:
    - Navigation icons: `"Home navigation icon"`, `"Search navigation icon"`.
    - Buttons: `"Refresh weather data"`, `"Request permissions"`.
    - Text fields: `"Enter city name input field"`.
    - Error/Loading sections: `"SkyCast Error message"`, `"Loading indicator"`.
  - **Purpose**: Ensures that screen readers can describe UI elements and actions to users with visual impairments.
  - **Consistency**: Semantic descriptions are tied to string resources (e.g., `R.string.home`, `R.string.refresh`) for localization support.

---

## 3. Technical Details

### Location Management
- **FusedLocationProviderClient**: Used for high-accuracy location updates via Google Play Services.
- **LocationCallback**: Updates `MainViewModel.currentLocation` with new `MyLatLng` values when a location is received.
- **Permission Handling**:
  - Uses `ActivityResultContracts.RequestMultiplePermissions` to request location permissions.
  - A `permissionLauncher` triggers the request on app launch if permissions are missing.
  - `LaunchedEffect` and a coroutine loop in `LocationScreen` continuously check permission status every 5 seconds.
- **Error Handling**:
  - If permissions are denied, `LocationScreen` shows an `ErrorSection` with a "Request Permissions" button.
  - If location services are disabled, an error message is displayed via `LocationUtils.isLocationEnabled`.

### State Management
- **MainViewModel States**:
  - `state`: Controls the Home screen (`NOTHING`, `LOADING`, `SUCCESS`, `FAILED`).
  - `searchState`: Controls the Search screen (`LOADING`, `SUCCESS`, `FAILED`).
  - `hasInitialFetchCompleted`: Ensures the initial weather fetch is complete before enabling auto-refresh.
- **Reactive Updates**:
  - `LaunchedEffect` hooks monitor changes in `state`, `searchState`, `darkMode`, `fontSizeScale`, and `language` to trigger recomposition.
  - Coroutines (`coroutineScope`, `Dispatchers.Main`) handle asynchronous tasks like location fetching and API calls.

### Coroutines and Asynchronous Operations
- **Usage**:
  - `fetchLocationWithTimeout` uses `suspendCancellableCoroutine` and `withTimeoutOrNull` to handle location requests with a timeout.
  - `LaunchedEffect` runs loops for periodic permission checks, location updates, and battery monitoring.
  - `coroutineScope.launch(Dispatchers.Main)` ensures UI updates are performed on the main thread.
- **Examples**:
  - Manual refresh button: Launches a coroutine to fetch location and weather data with retries.
  - Auto-refresh: Runs a loop in `LocationScreen` to update weather at intervals based on battery level.

### Battery Utilities
- **BatteryUtils**:
  - `observeBatteryLevel`: Returns the current battery percentage.
  - `shouldAutoRefresh`: Returns `false` if battery < 30%, triggering the manual refresh button.
- **Integration**: Battery status is checked in `LocationScreen` to determine `refreshInterval` and whether to show the refresh button.

### Language Support
- **LanguageUtils**:
  - `applySavedLocale`: Sets the app's locale on startup.
  - `getSavedLanguage`: Retrieves the saved language code (defaults to "en").
- **Restart Mechanism**: Language changes trigger an activity restart to apply the new locale, ensuring all string resources are updated.

---

## 4. UI Components

### Scaffold Layout
- **Structure**:
  - **TopAppBar**: Displays the app name (`R.string.app_name`) with a primary background.
  - **BottomBar**: Contains the `NavigationBar` and an optional `Refresh` button (shown when battery < 30% and location is enabled).
  - **Content**: Hosts the `NavHost` for screen content.
- **Accessibility**: The scaffold and its components include `contentDescription` for navigation and title.

### Home Screen (`LocationScreen`)
- **Logic**:
  - Checks permissions and location status.
  - Fetches initial location and weather data if not already completed.
  - Runs auto-refresh based on battery level or shows a manual refresh button.
- **UI States**:
  - Permissions denied: `ErrorSection` with a permission request button.
  - Location disabled: `ErrorSection` with a message.
  - Loading: `LoadingSection` with a `CircularProgressIndicator`.
  - Success: `WeatherSection` and `ForecastSection` with weather data.
  - Failed: `ErrorSection` with an error message.

### Search Screen (`SearchScreen`)
- **Logic**:
  - Captures city input and triggers weather/forecast API calls.
  - Updates UI based on `searchState`.
- **UI**:
  - `OutlinedTextField` for city input with custom colors and accessibility.
  - `Button` to initiate search, disabled during loading or if input is empty.
  - Conditional rendering of `LoadingSection`, `ErrorSection`, or weather/forecast sections.

### Settings Screen (`SettingsScreen`)
- **Logic**:
  - Manages font size, theme, and language settings via `MainViewModel`.
  - Launches `FunActivity` for the fun feature.
- **UI**:
  - Four `Card` components for font size, appearance, language, and fun feature.
  - Each card includes interactive elements (buttons, switch, dialog) with accessibility support.

### Reusable Components
- **ErrorSection**: Displays error messages with customizable color, centered in a `Column`.
- **LoadingSection**: Shows a centered `CircularProgressIndicator` for loading states.
- **WeatherSection** and **ForecastSection**: Render weather and forecast data (implementation details not shown but integrated with `MainViewModel` responses).

---

## 5. Error Handling and User Feedback
- **Permission Errors**: Displayed via `ErrorSection` with a retry button.
- **Location Errors**:
  - Invalid location (`lat == 0.0`, `lng == 0.0`): Retries every 5 seconds with a `Toast` message (`R.string.retrying_location`).
  - Location services disabled: Shows `ErrorSection` with `R.string.location_services_disabled`.
- **API Errors**:
  - `MainViewModel.state == STATE.FAILED`: Shows `ErrorSection` with `errorMessage`.
  - `searchState == STATE.FAILED`: Shows `ErrorSection` with `searchErrorMessage` and a `Toast`.
- **User Feedback**:
  - `Toast` messages for permission grant/denial, location retries, and search errors.
  - Clear UI states (loading, error, success) to guide users.

---

## 6. Optimization Techniques
- **Battery Efficiency**:
  - Dynamic refresh intervals based on battery level.
  - Manual refresh option for low battery to avoid background updates.
- **Location Efficiency**:
  - Uses `Priority.PRIORITY_HIGH_ACCURACY` with optimized intervals (3s min, 100ms max delay).
  - Stops location updates in `onPause` and resumes in `onResume`.
- **UI Performance**:
  - Jetpack Compose ensures efficient recomposition with `LaunchedEffect` and `remember`.
  - `rememberScrollState` enables smooth scrolling for long content.
- **State Management**:
  - Minimizes unnecessary recompositions by scoping `LaunchedEffect` to specific keys (e.g., `currentRoute`, `state`).
  - Uses `mutableStateOf` for reactive UI updates.

---

## 7. Limitations and Assumptions
- **Weather API**: The code references `getWeatherByLocation` and `getForecastByLocation` but does not show the API implementation. It likely uses a service like OpenWeatherMap, requiring an API key.
- **FunActivity**: The implementation of `FunActivity` is not provided, so its functionality is unknown.
- **Network Handling**: The code does not explicitly show retry logic or offline handling for API calls, which may be in `MainViewModel`.
- **Localization**: String resources (e.g., `R.string.home`) are used, but the full set of supported languages is not specified.

---

## Conclusion

The SkyCast app is a well-structured Android application that combines modern Android development practices with a user-friendly interface. It leverages **Jetpack Compose** for a responsive UI, **MVVM** for clean architecture, and **Coroutines** for efficient asynchronous operations. Key features like real-time weather, city search, and customizable settings are implemented with robust location handling, battery optimization, and accessibility support. The use of `FusedLocationProviderClient`, `Navigation Compose`, and dynamic theming ensures a polished user experience, while the fun feature adds an engaging element. The codebase is modular, maintainable, and optimized for performance, making it a strong example of a modern Android weather app.
