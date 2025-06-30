import org.gradle.api.Project
import kotlin.reflect.KProperty

/**
 * Delegation system for Gradle properties
 */
object PropertyDelegates {
    // Store reference to the project or null if not initialized
    private var projectRef: Project? = null
    
    /**
     * Get the project, throwing an error if not initialized
     */
    private val project: Project
        get() = projectRef ?: error("PropertyDelegates has not been initialized. Call PropertyDelegates.init(project) at the beginning of your build.gradle.kts file.")
    
    /**
     * Initialize the delegate system with a project reference
     */
    fun init(project: Project) {
        this.projectRef = project
    }
    
    /**
     * Base property delegate with lazy evaluation
     */
    abstract class PropertyDelegate(protected val key: String) {
        // Read only from local.properties file
        protected fun getPropertyValue(): Any? {
            val localPropsFile = project.rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                val properties = java.util.Properties()
                localPropsFile.inputStream().use { properties.load(it) }
                return properties.getProperty(key)
            }
            return null
        }
    }
    
    /**
     * Delegate for optional properties that can return null
     */
    class OptionalProperty(key: String) : PropertyDelegate(key) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
            return getPropertyValue() as? String
        }
    }
    
    /**
     * Delegate for required properties that will throw an error if not found
     */
    class RequiredProperty(key: String) : PropertyDelegate(key) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return getPropertyValue() as? String 
                ?: error("Required property $key not found")
        }
    }
    
    /**
     * Factory method for optional properties
     */
    fun optional(key: String) = OptionalProperty(key)
    
    /**
     * Factory method for required properties
     */
    fun required(key: String) = RequiredProperty(key)
}
