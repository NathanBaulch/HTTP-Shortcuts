package ch.rmy.android.http_shortcuts.usecases

import androidx.annotation.CheckResult
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.extensions.createDialogState
import javax.inject.Inject

class GetToolbarTitleChangeDialogUseCase
@Inject
constructor() {

    @CheckResult
    operator fun invoke(onToolbarTitleChangeSubmitted: (String) -> Unit, oldTitle: String) =
        createDialogState {
            title(R.string.title_set_title)
                .textInput(
                    prefill = oldTitle,
                    allowEmpty = true,
                    maxLength = TITLE_MAX_LENGTH,
                    callback = onToolbarTitleChangeSubmitted,
                )
                .build()
        }

    companion object {
        private const val TITLE_MAX_LENGTH = 50
    }
}
