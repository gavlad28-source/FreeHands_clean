package com.freehands.assistant.utils

/**
 * Contains constant values used throughout the application.
 */
object AppConstants {
    // App Info
    const val APP_NAME = "FreeHands"
    const val APP_DESCRIPTION = "Voice Assistant Application"
    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1
    
    // API Constants
    object Api {
        const val BASE_URL = "https://api.freehands.com/v1/"
        const val TIMEOUT = 30L // seconds
        const val MAX_RETRIES = 3
        
        // Endpoints
        const val ENDPOINT_VOICE_COMMANDS = "commands"
        const val ENDPOINT_USER_PROFILE = "user/profile"
        const val ENDPOINT_DEVICES = "devices"
        const val ENDPOINT_SETTINGS = "settings"
    }
    
    // Notification Constants
    object Notification {
        const val CHANNEL_ID_VOICE_COMMANDS = "voice_commands_channel"
        const val CHANNEL_ID_SERVICE = "background_service_channel"
        const val CHANNEL_ID_UPDATES = "updates_channel"
        
        const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1001
        const val NOTIFICATION_ID_VOICE_COMMAND = 1002
        const val NOTIFICATION_ID_UPDATE = 1003
        
        const val PENDING_INTENT_REQUEST_CODE = 2000
    }
    
    // Shared Preferences Keys
    object Prefs {
        const val PREF_NAME = "freehands_prefs"
        const val KEY_FIRST_LAUNCH = "is_first_launch"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_LAST_SYNC = "last_sync_timestamp"
    }
    
    // Request Codes
    object RequestCode {
        const val PERMISSION_RECORD_AUDIO = 100
        const val PERMISSION_READ_CONTACTS = 101
        const val PERMISSION_READ_CALENDAR = 102
        const val PERMISSION_READ_SMS = 103
        const val PERMISSION_READ_PHONE_STATE = 104
        const val PERMISSION_READ_CALL_LOG = 105
        const val PERMISSION_READ_EXTERNAL_STORAGE = 106
        const val PERMISSION_WRITE_EXTERNAL_STORAGE = 107
        const val PERMISSION_CAMERA = 108
        const val PERMISSION_LOCATION = 109
        
        const val VOICE_RECOGNITION = 200
        const val IMAGE_CAPTURE = 201
        const val IMAGE_PICK = 202
        const val FILE_PICK = 203
        const val CONTACT_PICK = 204
        
        const val BIOMETRIC_AUTH = 300
        const val DEVICE_ADMIN = 301
        const val ACCESSIBILITY_SETTINGS = 302
        const val NOTIFICATION_SETTINGS = 303
        const val APP_SETTINGS = 304
    }
    
    // Bundle/Intent Extras
    object Extras {
        const val EXTRA_COMMAND = "extra_command"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_VOICE_DATA = "extra_voice_data"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_CONTACT = "extra_contact"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_IS_SUCCESS = "extra_is_success"
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_RETRY_COUNT = "extra_retry_count"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_IS_COMPLETED = "extra_is_completed"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_TOTAL = "extra_total"
    }
    
    // Voice Command Constants
    object VoiceCommand {
        // Wake word and command structure
        const val WAKE_WORD = "hey freehands"
        const val COMMAND_PREFIX = "freehands"
        const val COMMAND_SEPARATOR = " "
        
        // Common command actions
        const val ACTION_OPEN = "open"
        const val ACTION_CLOSE = "close"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_SEND = "send"
        const val ACTION_CALL = "call"
        const val ACTION_TEXT = "text"
        const val ACTION_NAVIGATE = "navigate to"
        const val ACTION_SEARCH = "search for"
        
        // Command categories
        const val CATEGORY_SETTINGS = "settings"
        const val CATEGORY_APPS = "apps"
        const val CATEGORY_MEDIA = "media"
        const val CATEGORY_MESSAGES = "messages"
        const val CATEGORY_CALLS = "calls"
        const val CATEGORY_NAVIGATION = "navigation"
        
        // Settings commands
        const val SETTINGS_BRIGHTNESS = "brightness"
        const val SETTINGS_VOLUME = "volume"
        const val SETTINGS_WIFI = "wifi"
        const val SETTINGS_BLUETOOTH = "bluetooth"
        const val SETTINGS_AIRPLANE_MODE = "airplane mode"
        const val SETTINGS_FLASHLIGHT = "flashlight"
        
        // Media commands
        const val MEDIA_PLAY = "play"
        const val MEDIA_PAUSE = "pause"
        const val MEDIA_NEXT = "next"
        const val MEDIA_PREVIOUS = "previous"
        const val MEDIA_VOLUME_UP = "volume up"
        const val MEDIA_VOLUME_DOWN = "volume down"
        const val MEDIA_MUTE = "mute"
        
        // Message commands
        const val MESSAGE_READ = "read messages"
        const val MESSAGE_SEND = "send message to"
        const val MESSAGE_CALL = "call"
        const val MESSAGE_WHATSAPP = "whatsapp"
        const val MESSAGE_SMS = "text"
        
        // Navigation commands
        const val NAV_HOME = "home"
        const val NAV_WORK = "work"
        const val NAV_NEARBY = "nearby"
        const val NAV_GAS_STATION = "gas station"
        const val NAV_RESTAURANT = "restaurant"
        const val NAV_CAFE = "cafe"
        
        // System commands
        const val SYS_OPEN_APP = "open"
        const val SYS_CLOSE_APP = "close"
        const val SYS_GO_BACK = "go back"
        const val SYS_GO_HOME = "go home"
        const val SYS_OPEN_RECENT = "recent apps"
        const val SYS_TAKE_SCREENSHOT = "take screenshot"
        
        // Response templates
        const val RESPONSE_GREETING = "Hello! How can I help you today?"
        const val RESPONSE_NOT_UNDERSTOOD = "I'm sorry, I didn't understand that command. Could you please repeat?"
        const val RESPONSE_PROCESSING = "Processing your request..."
        const val RESPONSE_COMPLETED = "Done! Is there anything else I can help you with?"
        const val RESPONSE_ERROR = "I encountered an error. Please try again."
        // Command Types
        const val TYPE_CALL = "call"
        const val TYPE_MESSAGE = "message"
        const val TYPE_EMAIL = "email"
        const val TYPE_SEARCH = "search"
        const val TYPE_OPEN = "open"
        const val TYPE_CLOSE = "close"
        const val TYPE_SET = "set"
        const val TYPE_GET = "get"
        const val TYPE_CREATE = "create"
        const val TYPE_DELETE = "delete"
        const val TYPE_UPDATE = "update"
        const val TYPE_SEND = "send"
        const val TYPE_RECEIVE = "receive"
        
        // Command Parameters
        const val PARAM_TO = "to"
        const val PARAM_FROM = "from"
        const val PARAM_SUBJECT = "subject"
        const val PARAM_BODY = "body"
        const val PARAM_ATTACHMENT = "attachment"
        const val PARAM_WHEN = "when"
        const val PARAM_WHERE = "where"
        const val PARAM_WHY = "why"
        const val PARAM_HOW = "how"
        const val PARAM_WHAT = "what"
        const val PARAM_WHO = "who"
        
        // Command Responses
        const val RESPONSE_SUCCESS = "Success"
        const val RESPONSE_FAILURE = "I couldn't complete that request"
        const val RESPONSE_NOT_UNDERSTOOD = "I didn't understand that command"
        const val RESPONSE_MISSING_PARAM = "I need more information to complete that request"
        const val RESPONSE_NO_PERMISSION = "I don't have permission to do that"
        const val RESPONSE_NOT_SUPPORTED = "That feature is not supported yet"
        const val RESPONSE_TRY_AGAIN = "Please try again"
        const val RESPONSE_CANCELLED = "Request cancelled"
        const val RESPONSE_TIMEOUT = "Request timed out"
        const val RESPONSE_ERROR = "An error occurred"
        const val RESPONSE_READY = "I'm listening, how can I help?"
        const val RESPONSE_LISTENING = "Listening..."
        const val RESPONSE_PROCESSING = "Processing..."
        const val RESPONSE_COMPLETED = "Completed"
    }
    
    // Error Messages
    object Errors {
        const val NETWORK_ERROR = "Network error. Please check your connection and try again."
        const val SERVER_ERROR = "Server error. Please try again later."
        const val UNKNOWN_ERROR = "An unknown error occurred. Please try again."
        const val TIMEOUT_ERROR = "Request timed out. Please check your connection and try again."
        const val NO_INTERNET = "No internet connection. Please check your connection and try again."
        const val INVALID_RESPONSE = "Invalid response from server."
        const val INVALID_CREDENTIALS = "Invalid username or password."
        const val UNAUTHORIZED = "You are not authorized to perform this action."
        const val FORBIDDEN = "You don't have permission to access this resource."
        const val NOT_FOUND = "The requested resource was not found."
        const val CONFLICT = "A conflict occurred while processing your request."
        const val BAD_REQUEST = "The request was invalid or cannot be served."
        const val SERVICE_UNAVAILABLE = "The service is currently unavailable. Please try again later."
        const val GATEWAY_TIMEOUT = "The server is taking too long to respond. Please try again later."
    }
    
    // Date/Time Formats
    object DateTimeFormats {
        const val DATE_FORMAT = "yyyy-MM-dd"
        const val TIME_FORMAT = "HH:mm:ss"
        const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
        const val DATE_TIME_FORMAT_FILE = "yyyyMMdd_HHmmss"
        const val DATE_DISPLAY_FORMAT = "MMM d, yyyy"
        const val TIME_DISPLAY_FORMAT = "h:mm a"
        const val DATE_TIME_DISPLAY_FORMAT = "MMM d, yyyy h:mm a"
    }
    
    // File Constants
    object Files {
        const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        const val IMAGE_QUALITY = 80
        const val IMAGE_MAX_WIDTH = 1920
        const val IMAGE_MAX_HEIGHT = 1080
        const val CACHE_DIR = "cache"
        const val IMAGES_DIR = "images"
        const val AUDIO_DIR = "audio"
        const val DOCUMENTS_DIR = "documents"
        const val DOWNLOADS_DIR = "downloads"
        const val TEMP_DIR = "temp"
        const val BACKUP_DIR = "backups"
    }
    
    // Animation Durations
    object Animations {
        const val DURATION_SHORT = 200L
        const val DURATION_MEDIUM = 300L
        const val DURATION_LONG = 500L
        const val DURATION_EXTRA_LONG = 1000L
    }
    
    // Voice Recognition Constants
    object VoiceRecognition {
        // Audio configuration
        const val SAMPLE_RATE = 16000 // Hz
        const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE = 1024
        
        // Recognition settings
        const val MAX_RESULTS = 5
        const val CONFIDENCE_THRESHOLD = 0.3f
        const val SILENCE_DURATION_MS = 2000L
        const val ENDPOINTER_DURATION_AFTER_END_MS = 2000L
        
        // Audio source
        const val AUDIO_SOURCE = android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION
        
        // Recognition modes
        const val MODE_NORMAL = 0
        const val MODE_DICTATION = 1
        const val MODE_SEARCH = 2
        const val MODE_WEB_SEARCH = 3
        
        // Error codes
        const val ERROR_NETWORK_TIMEOUT = 1
        const val ERROR_NETWORK = 2
        const val ERROR_AUDIO = 3
        const val ERROR_SERVER = 4
        const val ERROR_CLIENT = 5
        const val ERROR_SPEECH_TIMEOUT = 6
        const val ERROR_NO_MATCH = 7
        const val ERROR_RECOGNIZER_BUSY = 8
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        
        // Intent extras
        const val EXTRA_LANGUAGE_MODEL = "android.speech.extra.LANGUAGE_MODEL"
        const val EXTRA_LANGUAGE = "android.speech.extra.LANGUAGE"
        const val EXTRA_PARTIAL_RESULTS = "android.speech.extra.PARTIAL_RESULTS"
        const val EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = "android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS"
        const val EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = "android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS"
        const val EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS = "android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS"
        const val EXTRA_PREFER_OFFLINE = "android.speech.extra.PREFER_OFFLINE"
        const val EXTRA_SECURE = "android.speech.extra.SECURE"
        const val EXTRA_CONFIDENCE_SCORES = "android.speech.extra.CONFIDENCE_SCORES"
        const val SILENCE_THRESHOLD = 0.1f
        const val MAX_RECORDING_TIME_MS = 30000L // 30 seconds
        const val MIN_RECORDING_TIME_MS = 1000L // 1 second
        const val SILENCE_DURATION_MS = 2000L // 2 seconds
    }
    
    // Text-to-Speech Constants
    object TextToSpeech {
        const val DEFAULT_SPEECH_RATE = 1.0f
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
        const val DEFAULT_PITCH = 1.0f
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2.0f
        const val DEFAULT_VOLUME = 1.0f
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
    }
    
    // Biometric Constants
    object Biometric {
        const val BIOMETRIC_STRONG = 0x00000FFF
        const val BIOMETRIC_WEAK = 0x0000FFFF
        const val DEVICE_CREDENTIAL = 0x0000F000
        const val AUTHENTICATION_DURATION = 30 // seconds
    }
    
    // Accessibility Constants
    object Accessibility {
        const val MIN_ACCESSIBILITY_INTERVAL_MS = 100L
        const val MAX_ACCESSIBILITY_EVENTS = 100
        const val SCROLL_STEP = 100 // pixels
        const val FLING_VELOCITY = 5000 // pixels per second
    }
    
    // WorkManager Constants
    object WorkManager {
        const val WORK_NAME_SYNC_DATA = "sync_data_work"
        const val WORK_NAME_BACKUP = "backup_work"
        const val WORK_TAG_SYNC = "sync_tag"
        const val WORK_TAG_BACKUP = "backup_tag"
        const val WORK_INTERVAL_HOURS = 24L // 24 hours
        const val WORK_FLEX_INTERVAL_HOURS = 1L // 1 hour
        const val WORK_BACKOFF_DELAY_MINUTES = 15L // 15 minutes
    }
    
    // Room Database Constants
    object Database {
        const val DB_NAME = "freehands_database"
        const val DB_VERSION = 1
        const val TABLE_VOICE_COMMANDS = "voice_commands"
        const val TABLE_USER_PROFILES = "user_profiles"
        const val TABLE_DEVICES = "devices"
        const val TABLE_SETTINGS = "settings"
        const val MIGRATION_1_2 = "MIGRATION_1_2"
    }
    
    // Encryption Constants
    object Encryption {
        const val KEYSTORE_ALIAS = "freehands_keystore_alias"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALGORITHM = "AES"
        const val BLOCK_MODE = "GCM"
        const val ENCRYPTION_PADDING = "NoPadding"
        const val ENCRYPTION_TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"
        const val IV_SEPARATOR = "\$iv$"
        const val KEY_SIZE = 256
        const val ITERATION_COUNT = 10000
        const val SALT_LENGTH = 32 // bytes
        const val IV_LENGTH = 12 // bytes for GCM
    }
    
    // Logging Constants
    object Logging {
        const val LOG_TAG = "FreeHands"
        const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10MB
        const val MAX_LOG_FILES = 5
        const val LOG_FILE_EXTENSION = ".log"
        const val LOG_DIR = "logs"
    }
    
    // Analytics Constants
    object Analytics {
        const val EVENT_BUTTON_CLICK = "button_click"
        const val EVENT_SCREEN_VIEW = "screen_view"
        const val EVENT_VOICE_COMMAND = "voice_command"
        const val EVENT_ERROR = "error"
        const val EVENT_EXCEPTION = "exception"
        const val EVENT_CRASH = "crash"
        
        const val PARAM_BUTTON_ID = "button_id"
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_COMMAND = "command"
        const val PARAM_SUCCESS = "success"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_EXCEPTION = "exception"
        const val PARAM_STACK_TRACE = "stack_trace"
    }
}
