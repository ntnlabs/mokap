package sk.ntnlabs.kyberia.parse

import org.jsoup.nodes.Document

data class UserInfo(
    /** ID vlastného profilu (odkaz „SELF“ v hlavičke). */
    val userId: Long?,
    /** Zostatok K_wallet, ak ho stránka ukazuje. */
    val kWallet: Long?,
)

object UserInfoParser {

    private val NODE_ID = Regex("""/id/(\d+)""")

    fun parse(document: Document): UserInfo {
        val self = document.select("a[href]").firstOrNull {
            it.attr("name").startsWith("SELF", ignoreCase = true) ||
                it.attr("title").startsWith("SELF", ignoreCase = true)
        }
        val userId = self?.attr("href")?.let { NODE_ID.find(it)?.groupValues?.get(1)?.toLongOrNull() }
        val kWallet = document.selectFirst("table.module_k_wallet")
            ?.select("td")
            ?.firstNotNullOfOrNull { it.text().filterNot(Char::isWhitespace).toLongOrNull() }
        return UserInfo(userId, kWallet)
    }
}
