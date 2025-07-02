package br.com.ticpass.pos.view.ui.login.states

class PasswordState :
    TextFieldState(validator = ::isPasswordValid, errorFor = ::passwordValidationError)

class ConfirmPasswordState(private val passwordState: PasswordState) : TextFieldState() {
    override val isValid
        get() = passwordAndConfirmationValid(passwordState.text)

    override fun getError(): String? {
        return if (showErrors()) {
            passwordConfirmationError()
        } else {
            null
        }
    }
}

private fun passwordAndConfirmationValid(password: String): Boolean {
    return isPasswordValid(password)
}

private fun isPasswordValid(password: String): Boolean {
    return password.length >= 6
}

@Suppress("UNUSED_PARAMETER")
private fun passwordValidationError(password: String): String {
    return "senha inválida"
}

private fun passwordConfirmationError(): String {
    return "as senhas preisam ser iguais"
}
