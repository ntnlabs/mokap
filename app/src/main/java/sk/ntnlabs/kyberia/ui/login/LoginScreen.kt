package sk.ntnlabs.kyberia.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sk.ntnlabs.kyberia.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    var username by rememberSaveable { mutableStateOf("") }
    // Heslo zámerne nie je v rememberSaveable – nesmie skončiť v uloženom stave aktivity.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val state = viewModel.state
    // ID je číslo – kým nie je, ani nemá zmysel posielať formulár.
    val usernameOk = if (state.byId) username.trim().let { it.isNotEmpty() && it.all(Char::isDigit) } else username.isNotBlank()
    val canSubmit = !state.loading && usernameOk && password.isNotEmpty()
    // Appka heslo neukladá; po úspešnom prihlásení len ponúkne uloženie správcovi hesiel v telefóne.
    val autofill = LocalAutofillManager.current
    val submit = { if (canSubmit) viewModel.login(username, password, onSuccess = { autofill?.commit() }) }

    LaunchedEffect(state.error) {
        if (state.error != null) password = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false to R.string.login_by_nick, true to R.string.login_by_id).forEachIndexed { index, (byId, label) ->
                SegmentedButton(
                    selected = state.byId == byId,
                    onClick = { viewModel.setById(byId) },
                    shape = SegmentedButtonDefaults.itemShape(index, count = 2),
                    enabled = !state.loading,
                ) { Text(stringResource(label)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(if (state.byId) R.string.login_user_id else R.string.login_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (state.byId) KeyboardType.Number else KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            supportingText = if (state.byId) {
                { Text(stringResource(R.string.login_id_hint)) }
            } else {
                null
            },
            // Správca hesiel (Google, Bitwarden…) vie pole vyplniť; ID je tiež prihlasovacie meno.
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painterResource(if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                        ),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
        )

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.login_button))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.login_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
