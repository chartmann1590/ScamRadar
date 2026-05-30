package com.charles.scamradar.app.recovery

import com.charles.scamradar.app.data.model.ScamType

sealed class AuthorityAction {
    data class Url(val url: String) : AuthorityAction()
    data class SmsCompose(val number: String, val body: String) : AuthorityAction()
    data class Dial(val number: String) : AuthorityAction()
}

data class Authority(
    val name: String,
    val description: String,
    val action: AuthorityAction,
)

object AuthorityDirectory {

    fun authoritiesFor(country: String, scamType: ScamType): List<Authority> {
        val upper = country.uppercase().take(2)
        return when (upper) {
            "US" -> usAuthorities(scamType)
            "GB", "UK" -> ukAuthorities(scamType)
            "AU" -> auAuthorities(scamType)
            "CA" -> caAuthorities(scamType)
            "IN" -> inAuthorities(scamType)
            else -> usAuthorities(scamType)
        }
    }

    private fun usAuthorities(scamType: ScamType): List<Authority> {
        val core = listOf(
            Authority(
                name = "FTC ReportFraud",
                description = "U.S. Federal Trade Commission — primary US scam report",
                action = AuthorityAction.Url("https://reportfraud.ftc.gov/"),
            ),
            Authority(
                name = "IC3 (FBI Internet Crime)",
                description = "Online/cyber fraud, including investment and crypto scams",
                action = AuthorityAction.Url("https://www.ic3.gov/Home/FileComplaint"),
            ),
            Authority(
                name = "BBB Scam Tracker",
                description = "Better Business Bureau — public scam reports database",
                action = AuthorityAction.Url("https://www.bbb.org/scamtracker"),
            ),
            Authority(
                name = "Carrier spam (7726)",
                description = "Forward the original SMS to 7726 (SPAM) on AT&T / Verizon / T-Mobile",
                action = AuthorityAction.SmsCompose("7726", ""),
            ),
        )
        val typeSpecific = when (scamType) {
            ScamType.IRS_IMPERSONATION -> listOf(
                Authority(
                    name = "TIGTA (IRS impersonation)",
                    description = "Treasury Inspector General — IRS scam phone calls",
                    action = AuthorityAction.Url("https://www.tigta.gov/reportcrime-misconduct"),
                ),
                Authority(
                    name = "IRS phishing email",
                    description = "Forward suspicious emails to phishing@irs.gov",
                    action = AuthorityAction.Url("mailto:phishing@irs.gov?subject=IRS%20impersonation"),
                ),
            )
            ScamType.INVESTMENT -> listOf(
                Authority(
                    name = "SEC Tip Line",
                    description = "Securities and Exchange Commission — investment fraud",
                    action = AuthorityAction.Url("https://www.sec.gov/tcr"),
                ),
            )
            ScamType.PACKAGE_DELIVERY -> listOf(
                Authority(
                    name = "USPIS (postal fraud)",
                    description = "U.S. Postal Inspection Service",
                    action = AuthorityAction.Url("https://www.uspis.gov/report"),
                ),
            )
            ScamType.ROMANCE, ScamType.FAMILY_EMERGENCY -> listOf(
                Authority(
                    name = "AARP Fraud Watch Helpline",
                    description = "Free helpline for elder-targeted fraud",
                    action = AuthorityAction.Dial("877-908-3360"),
                ),
            )
            else -> emptyList()
        }
        return core + typeSpecific
    }

    private fun ukAuthorities(scamType: ScamType): List<Authority> {
        val core = listOf(
            Authority(
                name = "Action Fraud",
                description = "UK national fraud reporting centre",
                action = AuthorityAction.Url("https://www.actionfraud.police.uk/reporting-fraud-and-cyber-crime"),
            ),
            Authority(
                name = "7726 forwarding",
                description = "Forward suspect SMS to 7726 free on UK networks",
                action = AuthorityAction.SmsCompose("7726", ""),
            ),
        )
        val typed = when (scamType) {
            ScamType.IRS_IMPERSONATION -> listOf(
                Authority(
                    name = "HMRC phishing",
                    description = "Forward HMRC-themed emails to phishing@hmrc.gov.uk",
                    action = AuthorityAction.Url("mailto:phishing@hmrc.gov.uk?subject=HMRC%20impersonation"),
                ),
            )
            else -> emptyList()
        }
        return core + typed
    }

    private fun auAuthorities(scamType: ScamType): List<Authority> {
        val core = listOf(
            Authority(
                name = "Scamwatch (ACCC)",
                description = "Australian Competition and Consumer Commission",
                action = AuthorityAction.Url("https://www.scamwatch.gov.au/report-a-scam"),
            ),
            Authority(
                name = "ReportCyber",
                description = "Australian Signals Directorate — cyber incidents",
                action = AuthorityAction.Url("https://www.cyber.gov.au/report-and-recover/report"),
            ),
        )
        val typed = when (scamType) {
            ScamType.IRS_IMPERSONATION -> listOf(
                Authority(
                    name = "ATO scam reports",
                    description = "Forward ATO-themed messages to ReportEmailFraud@ato.gov.au",
                    action = AuthorityAction.Url("mailto:ReportEmailFraud@ato.gov.au"),
                ),
            )
            else -> emptyList()
        }
        return core + typed
    }

    private fun caAuthorities(scamType: ScamType): List<Authority> {
        return listOf(
            Authority(
                name = "Canadian Anti-Fraud Centre",
                description = "Primary Canadian scam reporting",
                action = AuthorityAction.Url("https://antifraudcentre-centreantifraude.ca/report-signalez-eng.htm"),
            ),
            Authority(
                name = "CRA fraud (if tax-themed)",
                description = "Canada Revenue Agency online scam reporting",
                action = AuthorityAction.Url("https://www.canada.ca/en/revenue-agency/corporate/security/protect-yourself-against-fraud.html"),
            ),
        )
    }

    private fun inAuthorities(scamType: ScamType): List<Authority> {
        return listOf(
            Authority(
                name = "Cybercrime.gov.in",
                description = "Indian National Cyber Crime Reporting Portal",
                action = AuthorityAction.Url("https://cybercrime.gov.in/"),
            ),
            Authority(
                name = "1930 cyber helpline",
                description = "National helpline for ongoing financial fraud",
                action = AuthorityAction.Dial("1930"),
            ),
        )
    }
}
