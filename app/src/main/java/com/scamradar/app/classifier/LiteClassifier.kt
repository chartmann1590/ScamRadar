package com.scamradar.app.classifier

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scamradar.app.data.model.ClassifierTier
import com.scamradar.app.data.model.RedFlag
import com.scamradar.app.data.model.ScamType
import com.scamradar.app.data.model.ScanResult
import com.scamradar.app.data.model.Verdict
import java.io.InputStreamReader

data class PatternEntry(val pattern: String, val reason: String, val category: String, val weight: Float = 1.0f)

class LiteClassifier(
    private val gson: Gson = Gson()
) : ScamClassifier {

    override val name: String = "LiteClassifier"
    override val isAvailable: Boolean = true

    private val urlPatterns: List<PatternEntry> by lazy { loadBundledPatterns() }

    private val urgencyPatterns = listOf(
        PatternEntry("urgent", "Urgency language pressures victims to act without thinking", "urgency", 1.0f),
        PatternEntry("immediately", "Demands for immediate action are a common manipulation tactic", "urgency", 1.0f),
        PatternEntry("within \\d+ hours?", "Artificial time limits create unnecessary pressure", "urgency", 1.5f),
        PatternEntry("within \\d+ minutes?", "Extreme time pressure is a hallmark of scams", "urgency", 1.5f),
        PatternEntry("act now", "Classic urgency phrase designed to bypass critical thinking", "urgency", 1.2f),
        PatternEntry("act immediately", "Extreme urgency language used to prevent deliberation", "urgency", 1.3f),
        PatternEntry("expire", "Threat of expiration creates false urgency", "urgency", 1.0f),
        PatternEntry("suspend", "Threatening account suspension is a common scare tactic", "urgency", 1.2f),
        PatternEntry("suspended", "Claim of already-suspended account induces panic", "urgency", 1.3f),
        PatternEntry("final notice", "Fake final notices pressure victims into quick compliance", "urgency", 1.4f),
        PatternEntry("last warning", "Escalating warnings are designed to frighten victims", "urgency", 1.4f),
        PatternEntry("last chance", "Artificial deadline to prevent careful consideration", "urgency", 1.3f),
        PatternEntry("time sensitive", "False time sensitivity pressures immediate action", "urgency", 1.2f),
        PatternEntry("deadline", "Artificial deadlines prevent victims from verifying claims", "urgency", 1.0f),
        PatternEntry("before it'?s? too late", "Classic fear-based urgency phrase", "urgency", 1.3f),
        PatternEntry("don'?t? wait", "Discourages deliberation and verification", "urgency", 1.0f),
        PatternEntry("do not ignore", "Commands attention through fear", "urgency", 1.1f),
        PatternEntry("ignore at your own (?:risk|peril)", "Intimidation tactic to force compliance", "urgency", 1.3f),
        PatternEntry("your account will be", "Vague threats about account status", "urgency", 1.2f),
        PatternEntry("unauthorized (?:log|sign)?-?in attempt", "Fake security alerts create panic", "urgency", 1.4f),
        PatternEntry("unusual activity", "Vague security alert designed to alarm", "urgency", 1.2f),
        PatternEntry("verify your account", "Phishing prompt disguised as security measure", "urgency", 1.3f),
        PatternEntry("confirm your identity", "Identity harvesting under the guise of security", "urgency", 1.3f),
        PatternEntry("respond (?:asap|immediately|right away)", "Pressure to respond quickly without verifying", "urgency", 1.2f),
        PatternEntry("attention required", "Alarmist language to grab attention", "urgency", 1.0f),
        PatternEntry("important(?:!+|:)", "Exaggerated importance to override skepticism", "urgency", 0.8f),
        PatternEntry("failure to.*(?:respond|comply|act)", "Legalistic threats to intimidate victims", "urgency", 1.4f),
        PatternEntry("legal action", "Threats of legal consequences to coerce compliance", "urgency", 1.5f),
        PatternEntry("warrant", "Fake legal threats involving warrants", "urgency", 1.6f),
        PatternEntry("law enforcement", "Impersonation of law enforcement to intimidate", "urgency", 1.5f),
        PatternEntry("penalties? (?:and|&) fees", "Financial threats to pressure compliance", "urgency", 1.3f),
        PatternEntry("overdue", "Claims of overdue payments create anxiety", "urgency", 1.0f),
        PatternEntry("past due", "Fake past-due notices demand immediate payment", "urgency", 1.0f),
        PatternEntry("collection(?:s)? agency", "Threats involving collections intimidate victims", "urgency", 1.3f),
        PatternEntry("credit score", "Threats to credit score create financial fear", "urgency", 1.1f),
        PatternEntry("freeze (?:your )?(?:account|assets|funds)", "Threats to freeze financial resources", "urgency", 1.4f)
    )
    private val impersonationPatterns = listOf(
        PatternEntry("\\birs\\b", "IRS impersonation is a common government scam", "impersonation", 1.5f),
        PatternEntry("internal revenue", "Impersonation of the Internal Revenue Service", "impersonation", 1.5f),
        PatternEntry("u\\.?s\\.?p\\.?s", "USPS impersonation appears in package delivery scams", "impersonation", 1.3f),
        PatternEntry("united states postal", "Full name impersonation of the US Postal Service", "impersonation", 1.3f),
        PatternEntry("postal service", "Claims of being from the Postal Service should be verified", "impersonation", 1.2f),
        PatternEntry("fedex", "FedEx impersonation is used in delivery scams", "impersonation", 1.3f),
        PatternEntry("federal express", "Full name impersonation of Federal Express", "impersonation", 1.3f),
        PatternEntry("ups", "UPS impersonation appears in package delivery scams", "impersonation", 1.1f),
        PatternEntry("united parcel", "Full name impersonation of United Parcel Service", "impersonation", 1.3f),
        PatternEntry("dhl", "DHL impersonation is used in international delivery scams", "impersonation", 1.3f),
        PatternEntry("\\bbank\\b", "Bank impersonation is used to steal credentials", "impersonation", 1.2f),
        PatternEntry("\\bbanks?\\b", "Bank impersonation to harvest login credentials", "impersonation", 1.1f),
        PatternEntry("chase(?: bank)?", "Chase Bank impersonation for credential theft", "impersonation", 1.3f),
        PatternEntry("wells fargo", "Wells Fargo impersonation for credential theft", "impersonation", 1.3f),
        PatternEntry("bank of america", "Bank of America impersonation for credential theft", "impersonation", 1.3f),
        PatternEntry("citi(?:bank|card)?", "Citibank impersonation for credential theft", "impersonation", 1.3f),
        PatternEntry("netflix", "Netflix impersonation is used to steal payment info", "impersonation", 1.4f),
        PatternEntry("amazon", "Amazon impersonation is one of the most common brand scams", "impersonation", 1.3f),
        PatternEntry("(?:apple|icloud)", "Apple/iCloud impersonation is used for credential theft", "impersonation", 1.3f),
        PatternEntry("paypal", "PayPal impersonation targets financial credentials", "impersonation", 1.4f),
        PatternEntry("microsoft", "Microsoft impersonation appears in tech support scams", "impersonation", 1.3f),
        PatternEntry("windows (?:support|help desk)", "Windows support impersonation in tech support scams", "impersonation", 1.4f),
        PatternEntry("google (?:support|team|security)", "Google impersonation for credential harvesting", "impersonation", 1.3f),
        PatternEntry("facebook|meta", "Facebook/Meta impersonation for account theft", "impersonation", 1.2f),
        PatternEntry("social security", "Social Security Administration impersonation targets seniors", "impersonation", 1.6f),
        PatternEntry("\\bssa\\b", "SSA abbreviation used in Social Security scams", "impersonation", 1.5f),
        PatternEntry("medicare", "Medicare impersonation targets elderly victims", "impersonation", 1.4f),
        PatternEntry("medicaid", "Medicaid impersonation for identity theft", "impersonation", 1.4f),
        PatternEntry("fbi", "FBI impersonation is used to intimidate victims", "impersonation", 1.6f),
        PatternEntry("federal (?:bureau|agent|authority)", "Federal agency impersonation to intimidate", "impersonation", 1.5f),
        PatternEntry("police (?:department|officer|dept)", "Police impersonation threatens legal consequences", "impersonation", 1.5f),
        PatternEntry("(?:sheriff|deputy|detective)", "Law enforcement impersonation to coerce compliance", "impersonation", 1.5f),
        PatternEntry("immigration|ice|uscis", "Immigration agency impersonation targets vulnerable populations", "impersonation", 1.6f),
        PatternEntry("(?:at&t|verizon|t-mobile|sprint)", "Carrier impersonation for account or payment theft", "impersonation", 1.3f),
        PatternEntry(" Geek Squad", "Geek Squad impersonation is a widespread tech support scam", "impersonation", 1.5f)
    )
    private val moneyPatterns = listOf(
        PatternEntry("wire transfer", "Wire transfers are irreversible and commonly used in scams", "money", 1.5f),
        PatternEntry("gift card", "Requests for gift card payments are almost always scams", "money", 1.8f),
        PatternEntry("bitcoin", "Cryptocurrency requests are hard to trace and recover", "money", 1.3f),
        PatternEntry("btc", "Abbreviation for Bitcoin often used in scam communications", "money", 1.2f),
        PatternEntry("cryptocurrency", "Cryptocurrency payment requests are a modern scam vector", "money", 1.3f),
        PatternEntry("western union", "Western Union is a common tool for wire fraud", "money", 1.6f),
        PatternEntry("prepaid card", "Prepaid card payments are untraceable and favored by scammers", "money", 1.7f),
        PatternEntry("green dot", "Green Dot cards are a known scam payment method", "money", 1.7f),
        PatternEntry("moneygram", "MoneyGram transfers are difficult to reverse", "money", 1.5f),
        PatternEntry("money pak", "MoneyPak is commonly used in prepaid card scams", "money", 1.7f),
        PatternEntry("send money", "Direct requests to send money are suspicious", "money", 1.2f),
        PatternEntry("transfer funds?", "Requests to transfer funds should be verified independently", "money", 1.3f),
        PatternEntry("payment (?:is )?required", "Demands for payment without proper verification", "money", 1.2f),
        PatternEntry("processing fee", "Upfront fees are a hallmark of advance-fee scams", "money", 1.6f),
        PatternEntry("upfront fee", "Legitimate services do not typically require upfront fees", "money", 1.7f),
        PatternEntry("advance fee", "Advance-fee fraud is one of the most common scam types", "money", 1.8f),
        PatternEntry("clearance fee", "Fake clearance fees are requested in lottery and inheritance scams", "money", 1.6f),
        PatternEntry("release fee", "Scammers charge fake release fees before disappearing", "money", 1.6f),
        PatternEntry("tax (?:and|&) clearance", "Fake tax clearance demands appear in inheritance scams", "money", 1.5f),
        PatternEntry("bank details?", "Requests for bank details are used for financial theft", "money", 1.4f),
        PatternEntry("account (?:and )?routing number", "Bank routing info is used to drain accounts", "money", 1.5f),
        PatternEntry("venmo", "Peer-to-peer payment requests from strangers are risky", "money", 1.0f),
        PatternEntry("zelle", "Zelle transfers to unknown recipients are irreversible", "money", 1.1f),
        PatternEntry("cash app", "Cash App requests from strangers may be fraudulent", "money", 1.0f),
        PatternEntry("paypal(?:\\.me)?", "Unsolicited PayPal payment links are suspicious", "money", 1.0f),
        PatternEntry("\\$\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})? (?:fee|charge|cost)", "Specific dollar amounts for fees are typical in scams", "money", 1.3f),
        PatternEntry("refund (?:of|for|amount)", "Fake refund offers are used to steal payment info", "money", 1.2f),
        PatternEntry("you (?:are|will be|have been) (?:entitled|awarded|selected)", "Claims of unclaimed money or prizes", "money", 1.4f),
        PatternEntry("unclaimed (?:funds|money|property)", "Fake unclaimed property notices harvest personal info", "money", 1.4f),
        PatternEntry("inheritance", "Unexpected inheritance offers are a classic advance-fee scam", "money", 1.6f),
        PatternEntry("beneficiary", "Fake beneficiary notifications appear in inheritance scams", "money", 1.4f),
        PatternEntry("won.*(?:lottery|prize|jackpot)", "Unexpected prize notifications are almost always scams", "money", 1.7f),
        PatternEntry("claim your (?:prize|winnings|reward)", "Requests to claim prizes are used to extract fees", "money", 1.6f)
    )
    private val personalInfoPatterns = listOf(
        PatternEntry("\\bssn\\b", "Requests for Social Security Numbers are used for identity theft", "personal_info", 1.6f),
        PatternEntry("social security number", "SSN requests are the top indicator of identity theft attempts", "personal_info", 1.7f),
        PatternEntry("social security #(?: ?number)?", "SSN requests disguised as routine verification", "personal_info", 1.7f),
        PatternEntry("\\bpassword\\b", "Legitimate services never ask for your password via message", "personal_info", 1.7f),
        PatternEntry("\\bpin\\b", "PIN requests are used to access financial accounts", "personal_info", 1.6f),
        PatternEntry("\\bpasscode\\b", "Passcode requests can be used to bypass two-factor authentication", "personal_info", 1.5f),
        PatternEntry("date of birth", "Date of birth requests are used for identity theft", "personal_info", 1.4f),
        PatternEntry("\\bdob\\b", "DOB abbreviation used for identity verification harvesting", "personal_info", 1.4f),
        PatternEntry("mother'?s? maiden name", "Maiden name is a common security question answer", "personal_info", 1.5f),
        PatternEntry("driver'?s? license", "Driver's license info is valuable for identity thieves", "personal_info", 1.4f),
        PatternEntry("passport number", "Passport numbers are targeted for identity theft", "personal_info", 1.4f),
        PatternEntry("bank account number", "Bank account numbers are used to drain funds", "personal_info", 1.6f),
        PatternEntry("routing number", "Routing numbers combined with account numbers enable theft", "personal_info", 1.5f),
        PatternEntry("credit card number", "Credit card number requests are used for financial fraud", "personal_info", 1.6f),
        PatternEntry("cvv", "CVV requests complete stolen credit card info", "personal_info", 1.7f),
        PatternEntry("card (?:security|verification) code", "Card security codes should never be shared via message", "personal_info", 1.6f),
        PatternEntry("login credentials", "Requests for login credentials are always fraudulent", "personal_info", 1.7f),
        PatternEntry("username and password", "Credential harvesting attempts steal account access", "personal_info", 1.7f),
        PatternEntry("security (?:questions?|answers?)", "Security question answers enable account takeovers", "personal_info", 1.5f),
        PatternEntry("home address", "Address collection is part of identity theft profiles", "personal_info", 1.2f),
        PatternEntry("full name", "Name collection combined with other data enables identity theft", "personal_info", 1.1f),
        PatternEntry("(?:copy|photo|picture|scan) of (?:your )?(?:id|identification|license)", "Requests for ID copies are used for identity fraud", "personal_info", 1.6f),
        PatternEntry("verify (?:your )?(?:identity|information|details)", "Identity verification requests often precede data theft", "personal_info", 1.3f),
        PatternEntry("update (?:your )?(?:information|records|details|profile)", "Fake update requests harvest personal data", "personal_info", 1.2f)
    )
    private val phishingUrlPatterns = listOf(
        PatternEntry("bit\\.ly/", "Shortened URLs hide the real destination and are common in phishing", "phishing_url", 1.2f),
        PatternEntry("tinyurl\\.com/", "URL shorteners mask malicious destinations", "phishing_url", 1.2f),
        PatternEntry("t\\.co/", "Shortened Twitter links can redirect to malicious sites", "phishing_url", 1.0f),
        PatternEntry("goo\\.gl/", "Google URL shortener can hide phishing destinations", "phishing_url", 1.1f),
        PatternEntry("ow\\.ly/", "Ow.ly short URLs can mask malicious destinations", "phishing_url", 1.1f),
        PatternEntry("is\\.gd/", "Is.gd short URLs can redirect to phishing sites", "phishing_url", 1.1f),
        PatternEntry("\\.tk/", ".tk domains are free and frequently abused by scammers", "phishing_url", 1.3f),
        PatternEntry("\\.ml/", ".ml domains are free and often used in phishing", "phishing_url", 1.3f),
        PatternEntry("\\.ga/", ".ga domains are free and commonly abused", "phishing_url", 1.3f),
        PatternEntry("\\.cf/", ".cf domains are free and used in phishing campaigns", "phishing_url", 1.3f),
        PatternEntry("\\.gq/", ".gq domains are free and associated with spam", "phishing_url", 1.3f),
        PatternEntry("\\.xyz", ".xyz domains are cheap and frequently used in scams", "phishing_url", 1.1f),
        PatternEntry("\\.top", ".top domains are commonly used in phishing campaigns", "phishing_url", 1.1f),
        PatternEntry("\\.buzz", ".buzz domains are suspicious in unsolicited messages", "phishing_url", 1.0f),
        PatternEntry("\\.click", ".click domains in messages often lead to phishing pages", "phishing_url", 1.1f),
        PatternEntry("\\.loan", ".loan domains are frequently associated with scams", "phishing_url", 1.2f),
        PatternEntry("\\.work", ".work domains are suspicious in unsolicited links", "phishing_url", 1.0f),
        PatternEntry("\\.date", ".date domains are commonly used in phishing", "phishing_url", 1.0f),
        PatternEntry("\\.trade", ".trade domains are frequently used by scammers", "phishing_url", 1.0f),
        PatternEntry("\\.men", ".men domains are commonly associated with scam sites", "phishing_url", 1.0f),
        PatternEntry("\\.stream", ".stream domains are suspicious in unsolicited messages", "phishing_url", 1.0f),
        PatternEntry("\\.download", ".download domains in links are highly suspicious", "phishing_url", 1.2f),
        PatternEntry("(?:usps|fedex|ups|dhl|amazon|paypal|bank|chase|wellsfargo|apple|netflix)[.-]", "Brand name in domain that is not the official domain", "phishing_url", 1.5f),
        PatternEntry("\\b(?:usps|fedex|ups|amazon|paypal|apple|netflix|bank)\\w*\\.(?!com|net|org)", "Brand impersonation in non-official domain TLD", "phishing_url", 1.5f),
        PatternEntry("http://[^\\s]+(?:usps|fedex|ups|amazon|paypal|apple|netflix)", "HTTP URL containing brand name (not HTTPS official site)", "phishing_url", 1.6f),
        PatternEntry("\\b(?:secure|login|verify|update|confirm|account|support|help)\\w*\\.(?!com|net|org)", "Action words in suspicious domain names", "phishing_url", 1.2f),
        PatternEntry("-(?:secure|login|verify|update|confirm|support)\\.", "Hyphenated action words in domain are suspicious", "phishing_url", 1.3f),
        PatternEntry("\\b\\w+-(?:support|help|service|care)\\.", "Customer service subdomain patterns used in phishing", "phishing_url", 1.2f),
        PatternEntry("[a-z]{15,}\\.(?:com|net|org)", "Unusually long domain names are often auto-generated for phishing", "phishing_url", 1.0f),
        PatternEntry("(?:\\d{1,3}\\.){3}\\d{1,3}", "Raw IP addresses in URLs are highly suspicious", "phishing_url", 1.8f),
        PatternEntry("@\\w+\\.\\w+", "URL with @ symbol can redirect to a different site", "phishing_url", 1.3f),
        PatternEntry("(?:arnaz0n|arnazon|amazo[o0]n|amaz0n)", "Misspelled Amazon domain for phishing", "phishing_url", 1.7f),
        PatternEntry("(?:paypa[l1]|paypa1|paypla)", "Misspelled PayPal domain for phishing", "phishing_url", 1.7f),
        PatternEntry("(?:netfli[xv]|netfl1x|netfliz)", "Misspelled Netflix domain for phishing", "phishing_url", 1.7f),
        PatternEntry("(?:app[e3]l|app1e|appl[e3]support)", "Misspelled Apple domain for phishing", "phishing_url", 1.7f),
        PatternEntry("(?:g[o0]{2}gle|g00gle|goog[e3]l)", "Misspelled Google domain for phishing", "phishing_url", 1.6f),
        PatternEntry("(?:faceb[o0]{2}k|faceb00k|facebok)", "Misspelled Facebook domain for phishing", "phishing_url", 1.6f),
        PatternEntry("(?:m[i1]cr[o0]s[o0]ft|micr0soft|microsoft[-]support)", "Misspelled Microsoft domain for phishing", "phishing_url", 1.6f)
    )
    private val romancePatterns = listOf(
        PatternEntry("my love", "Premature declarations of love from strangers are a romance scam indicator", "romance", 1.3f),
        PatternEntry("my darling", "Terms of endearment from unknown contacts suggest romance scams", "romance", 1.3f),
        PatternEntry("my soulmate", "Soulmate claims from strangers are a hallmark of romance scams", "romance", 1.4f),
        PatternEntry("i have been (?:looking|searching|waiting) for you", "Targeted flattery used to build false emotional connection", "romance", 1.3f),
        PatternEntry("you are (?:the one|special|different|everything)", "Excessive flattery to establish emotional dependency", "romance", 1.2f),
        PatternEntry("god (?:sent|brought|made) you", "Religious framing used to build trust in romance scams", "romance", 1.3f),
        PatternEntry("trust(?:ing)? (?:me|you|in me)", "Explicit trust demands are a manipulation technique", "romance", 1.2f),
        PatternEntry("i (?:am|have) fallen for you", "Rapid emotional attachment is a romance scam red flag", "romance", 1.3f),
        PatternEntry("can'?t? live without you", "Extreme emotional manipulation to create dependency", "romance", 1.4f),
        PatternEntry("future together", "Premature future plans from strangers indicate romance scams", "romance", 1.3f),
        PatternEntry("overseas", "Claims of being overseas are common in romance scams", "romance", 1.2f),
        PatternEntry("(?:deployed|military|soldier|army) (?:in|overseas|abroad)", "Military impersonation combined with overseas claims", "romance", 1.5f),
        PatternEntry("working (?:in|on a) (?:contract|project) (?:abroad|overseas|in africa|in the uk)", "Overseas work claims limit ability to verify identity", "romance", 1.4f),
        PatternEntry("widow(?:er)?", "Fake widower status generates sympathy and trust", "romance", 1.3f),
        PatternEntry("lost (?:my )?(?:wife|husband|spouse|partner)", "Fake bereavement stories build emotional connection", "romance", 1.3f),
        PatternEntry("inheritance.*(?:stuck|frozen|held)", "Inheritance stories combined with romance are scam indicators", "romance", 1.6f),
        PatternEntry("need.*(?:money|help|financial assistance)", "Financial requests following emotional bonding", "romance", 1.5f),
        PatternEntry("flight (?:ticket|to come see you|to visit)", "Fake travel plans to justify requesting money", "romance", 1.4f),
        PatternEntry("(?:send|wire) (?:money|funds) for.*(?:flight|ticket|visa|passport)", "Requests for travel money are a classic romance scam", "romance", 1.6f),
        PatternEntry("webcam.*(?:broken|not working|damaged)", "Excuses to avoid video verification of identity", "romance", 1.4f),
        PatternEntry("camera.*(?:broken|not working|damaged|cracked)", "Camera excuses prevent identity verification", "romance", 1.4f),
        PatternEntry("phone.*(?:broken|stolen|damaged|confiscated)", "Phone problems prevent communication verification", "romance", 1.2f)
    )
    private val investmentPatterns = listOf(
        PatternEntry("guaranteed (?:return|profit|income|ROI)", "No investment can guarantee returns", "investment", 1.6f),
        PatternEntry("risk[- ]?free", "No investment is truly risk-free", "investment", 1.6f),
        PatternEntry("\\d+% (?:return|profit|ROI|gain|interest)", "Unrealistically high returns promised", "investment", 1.7f),
        PatternEntry("double (?:your )?(?:money|investment|bitcoin)", "Promises to double money are always scams", "investment", 1.8f),
        PatternEntry("get rich quick", "Get-rich-quick schemes always benefit the promoter", "investment", 1.7f),
        PatternEntry("passive income", "Unsolicited passive income offers are suspicious", "investment", 1.3f),
        PatternEntry("forex (?:trading|signal|robot|bot)", "Forex trading offers from strangers are high-risk", "investment", 1.4f),
        PatternEntry("binary option", "Binary options are largely unregulated and commonly scammed", "investment", 1.6f),
        PatternEntry("day trading (?:group|community|signal)", "Unsolicited day trading groups are often pump-and-dump", "investment", 1.4f),
        PatternEntry("pump and dump", "Pump-and-dump schemes defraud retail investors", "investment", 1.8f),
        PatternEntry("ico\\b", "Initial coin offerings are frequently fraudulent", "investment", 1.4f),
        PatternEntry("nft (?:giveaway|airdrop|mint)", "Fake NFT offers steal wallet credentials", "investment", 1.4f),
        PatternEntry("(?:crypto|bitcoin|btc) (?:investment|opportunity|platform)", "Crypto investment opportunities from strangers are scams", "investment", 1.5f),
        PatternEntry("trading (?:bot|algorithm|platform|software)", "Automated trading promises are often fraudulent", "investment", 1.4f),
        PatternEntry("insider (?:tip|information|trading|knowledge)", "Insider trading tips are illegal and likely scams", "investment", 1.6f),
        PatternEntry("limited (?:time|seats|spots|availability)", "Artificial scarcity to rush investment decisions", "investment", 1.3f),
        PatternEntry("exclusive (?:opportunity|access|group|club)", "False exclusivity to create FOMO", "investment", 1.3f),
        PatternEntry("whale (?:group|alert|signal)", "Crypto whale group promises are scams", "investment", 1.5f),
        PatternEntry("signal (?:group|channel|service)", "Trading signal services from unknown sources are unreliable", "investment", 1.3f),
        PatternEntry("copy (?:trading|my trades)", "Copy trading invitations from strangers are risky", "investment", 1.3f),
        PatternEntry("(?:make|earn) .* (?:per|every) (?:day|week|month|hour)", "Unrealistic earning promises with specific timeframes", "investment", 1.5f),
        PatternEntry("financial freedom", "Vague financial freedom promises are used to lure victims", "investment", 1.3f),
        PatternEntry("retire (?:early|young|now|tomorrow)", "Unrealistic retirement promises from investments", "investment", 1.3f),
        PatternEntry("multi[- ]?level (?:marketing|business)", "MLM structures often function as pyramid schemes", "investment", 1.4f),
        PatternEntry("pyramid", "Pyramid schemes are illegal and unsustainable", "investment", 1.8f)
    )
    private val familyEmergencyPatterns = listOf(
        PatternEntry("your (?:son|daughter|child|kid)", "References to your child create immediate emotional distress", "family_emergency", 1.4f),
        PatternEntry("(?:grandchild|grandson|granddaughter|grandkid)", "Grandchild references target elderly victims", "family_emergency", 1.5f),
        PatternEntry("in jail", "Claims of a family member in jail create panic", "family_emergency", 1.6f),
        PatternEntry("in (?:the )?hospital", "Hospital claims about family members create urgency", "family_emergency", 1.5f),
        PatternEntry("in (?:an? )?accident", "Accident claims about family members are distressing", "family_emergency", 1.5f),
        PatternEntry("bail (?:money|bond)?", "Bail money requests are a common emergency scam", "family_emergency", 1.7f),
        PatternEntry("bail (?:is|amount|set at)", "Specific bail amounts add false credibility", "family_emergency", 1.7f),
        PatternEntry("hospital bills?", "Hospital bill requests exploit family concern", "family_emergency", 1.6f),
        PatternEntry("medical (?:emergency|bills|expenses|treatment)", "Medical emergency claims demand immediate financial response", "family_emergency", 1.5f),
        PatternEntry("surgery", "Emergency surgery claims create extreme urgency", "family_emergency", 1.6f),
        PatternEntry("(?:need|send) (?:money|cash) (?:right |urgent)?(?:now|immediately|asap)", "Immediate money requests following emergency claims", "family_emergency", 1.6f),
        PatternEntry("i'?m? (?:am )?(?:in trouble|hurt|injured|stranded|stuck)", "Personal distress claims from supposedly known contacts", "family_emergency", 1.4f),
        PatternEntry("(?:car |auto )?accident", "Car accident claims are used to explain sudden need for money", "family_emergency", 1.4f),
        PatternEntry("arrested", "Claims of arrest create legal fear and urgency", "family_emergency", 1.5f),
        PatternEntry("kidnapped", "Kidnapping claims cause extreme panic", "family_emergency", 1.8f),
        PatternEntry("(?:don'?t?|do not) tell (?:mom|dad|anyone|the family)", "Secrecy demands prevent victims from verifying the story", "family_emergency", 1.7f),
        PatternEntry("(?:my|your) (?:phone|cell) (?:is )?(?:broken|dead|dying|lost|stolen)", "Phone excuses prevent voice verification", "family_emergency", 1.5f),
        PatternEntry("this is (?:your |my )?(?:son|daughter|grandson|granddaughter|nephew|niece)", "Identity claims without proper verification", "family_emergency", 1.4f),
        PatternEntry("(?:i(?:'?m)?|i am) (?:using|on) (?:a|someone'?s?|my friend'?s?) (?:friend|different|new|another) (?:phone|device|number)", "Claims of using a different phone prevent callback verification", "family_emergency", 1.5f),
        PatternEntry("wire (?:money|funds) (?:to|for) (?:me|my? (?:lawyer|doctor|friend))", "Requests to wire money to a third party are suspicious", "family_emergency", 1.6f)
    )

    private val techSupportPatterns = listOf(
        PatternEntry("(?:your )?(?:computer|pc|laptop|device) (?:is|has been) (?:infected|compromised|hacked|blocked)", "Fake virus alerts frighten users into calling scammers", "tech_support", 1.6f),
        PatternEntry("(?:virus|malware|spyware|trojan|ransomware) (?:detected|found|alert|warning)", "Fake malware alerts pressure victims to call for help", "tech_support", 1.5f),
        PatternEntry("pop[- ]?up (?:message|alert|warning|notification)", "Tech support scammers use fake pop-ups as bait", "tech_support", 1.3f),
        PatternEntry("call (?:this number|us|now|immediately|right away) (?:at)?", "Urgent call-to-action numbers lead to scam call centers", "tech_support", 1.4f),
        PatternEntry("toll[- ]?free (?:number|support|helpline)", "Fake toll-free numbers connect to scam operations", "tech_support", 1.3f),
        PatternEntry("remote (?:access|desktop|support|connection)", "Remote access grants scammers control of your device", "tech_support", 1.7f),
        PatternEntry("(?:screen|device) (?:lock|locked|frozen)", "Fake screen lock alerts simulate security breaches", "tech_support", 1.5f),
        PatternEntry("firewall (?:breach|warning|alert|expired)", "Fake firewall alerts create security anxiety", "tech_support", 1.4f),
        PatternEntry("ip address.*(?:compromised|hacked|breached|infected)", "IP address scare tactics are technically meaningless", "tech_support", 1.6f),
        PatternEntry("network (?:security|breach|compromised|hack)", "Network security scare alerts are unspecific and manipulative", "tech_support", 1.4f),
        PatternEntry("(?:error|warning) code", "Fake error codes add false technical credibility", "tech_support", 1.3f),
        PatternEntry("(?:refund|compensation) (?:for|from) (?:microsoft|windows|geek squad|tech support)", "Fake tech support refunds are a reversal scam", "tech_support", 1.7f)
    )

    private val jobOfferPatterns = listOf(
        PatternEntry("work from home", "Work-from-home offers from unknown sources are often scams", "job_offer", 1.1f),
        PatternEntry("(?:earn|make) .* (?:weekly|daily|per hour|per week)", "Unrealistic earning promises in job offers", "job_offer", 1.3f),
        PatternEntry("no experience (?:needed|required|necessary)", "Jobs requiring no experience but promising high pay are suspicious", "job_offer", 1.3f),
        PatternEntry("mystery shopper", "Mystery shopper scams involve fake checks", "job_offer", 1.5f),
        PatternEntry("personal assistant", "Personal assistant job scams target individuals for money laundering", "job_offer", 1.4f),
        PatternEntry("(?:cash|deposit|receive|process) (?:checks?|payments?|money orders?)", "Payment processing roles are often money laundering fronts", "job_offer", 1.7f),
        PatternEntry("package (?:forwarding|reshipping|inspection)", "Package reshipping jobs are part of organized fraud rings", "job_offer", 1.7f),
        PatternEntry("(?:overpayment|overpay) (?:you|check|for)", "Overpayment scams involve sending fake checks", "job_offer", 1.6f),
        PatternEntry("(?:buy|purchase|shop) (?:for us|items|products|electronics)", "Purchase-based job scams involve stolen credit cards", "job_offer", 1.5f),
        PatternEntry("commission[- ]?based", "Commission-only jobs can mask pyramid schemes", "job_offer", 1.2f),
        PatternEntry("direct deposit.*(?:info|details|form)", "Direct deposit info collection is used for financial theft", "job_offer", 1.4f),
        PatternEntry("background check fee", "Legitimate employers do not charge background check fees to applicants", "job_offer", 1.6f),
        PatternEntry("training (?:fee|cost|materials)", "Upfront training costs are a sign of job scams", "job_offer", 1.5f),
        PatternEntry("equipment (?:fee|deposit|cost)", "Equipment fees for jobs are typically scams", "job_offer", 1.5f),
        PatternEntry("i[ft]t? (?:is|was) (?:recommended|suggested) (?:that )?you", "Vague recommendations without specific referrals", "job_offer", 1.2f)
    )
    private val allPatternCategories: Map<String, List<PatternEntry>> by lazy {
        mutableMapOf<String, List<PatternEntry>>(
            "urgency" to urgencyPatterns,
            "money" to moneyPatterns,
            "impersonation" to impersonationPatterns,
            "phishing_url" to phishingUrlPatterns,
            "personal_info" to personalInfoPatterns,
            "romance" to romancePatterns,
            "investment" to investmentPatterns,
            "family_emergency" to familyEmergencyPatterns,
            "tech_support" to techSupportPatterns,
            "job_offer" to jobOfferPatterns
        ).also { map ->
            urlPatterns.forEach { entry ->
                map["phishing_url"] = (map["phishing_url"] ?: emptyList()) + entry
            }
        }
    }

    private fun loadBundledPatterns(): List<PatternEntry> {
        return try {
            val type = object : TypeToken<List<PatternEntry>>() {}.type
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun matchPatterns(message: String, patterns: List<PatternEntry>): List<MatchResult> {
        val results = mutableListOf<MatchResult>()
        for (entry in patterns) {
            val regex = Regex(entry.pattern, RegexOption.IGNORE_CASE)
            val matches = regex.findAll(message)
            for (match in matches) {
                results.add(MatchResult(entry, match.value))
            }
        }
        return results
    }

    data class MatchResult(val entry: PatternEntry, val matchedText: String)

    override suspend fun classify(message: String): ScanResult {
        val allMatches = mutableListOf<MatchResult>()

        for ((_, patterns) in allPatternCategories) {
            allMatches.addAll(matchPatterns(message, patterns))
        }

        val categoryScores = mutableMapOf<String, Float>()
        val redFlags = mutableListOf<RedFlag>()
        val seenPhrases = mutableSetOf<String>()

        for (match in allMatches) {
            val cat = match.entry.category
            categoryScores[cat] = (categoryScores[cat] ?: 0f) + match.entry.weight

            if (match.matchedText !in seenPhrases) {
                seenPhrases.add(match.matchedText)
                redFlags.add(RedFlag(match.matchedText, match.entry.reason))
            }
        }

        val totalScore = categoryScores.values.sum()

        val verdict = when {
            totalScore >= 6.0f -> Verdict.LIKELY_SCAM
            totalScore >= 2.5f -> Verdict.SUSPICIOUS
            else -> Verdict.SAFE
        }

        val confidence = (totalScore / (totalScore + 4.0f)).coerceIn(0f, 1f)

        val scamType = determineScamType(categoryScores)

        val recommendedAction = when (verdict) {
            Verdict.SAFE -> "This message appears to be safe, but always remain cautious."
            Verdict.SUSPICIOUS -> "This message has suspicious indicators. Verify the sender through official channels before taking any action."
            Verdict.LIKELY_SCAM -> "This message is very likely a scam. Do not respond, click any links, or provide any personal information. Report and delete."
        }

        return ScanResult(
            verdict = verdict,
            confidence = confidence,
            scamType = scamType,
            redFlags = redFlags,
            aiGeneratedIndicators = emptyList(),
            recommendedAction = recommendedAction,
            originalMessage = message,
            timestamp = System.currentTimeMillis(),
            classifierTier = ClassifierTier.LITE
        )
    }

    private fun determineScamType(categoryScores: Map<String, Float>): ScamType {
        val maxEntry = categoryScores.maxByOrNull { it.value } ?: return ScamType.NONE
        return when (maxEntry.key) {
            "phishing_url" -> ScamType.PHISHING
            "impersonation" -> ScamType.PHISHING
            "romance" -> ScamType.ROMANCE
            "investment" -> ScamType.INVESTMENT
            "family_emergency" -> ScamType.FAMILY_EMERGENCY
            "tech_support" -> ScamType.TECH_SUPPORT
            "job_offer" -> ScamType.JOB_OFFER
            "money" -> {
                val investmentScore = categoryScores["investment"] ?: 0f
                val romanceScore = categoryScores["romance"] ?: 0f
                when {
                    investmentScore > 1f -> ScamType.INVESTMENT
                    romanceScore > 1f -> ScamType.ROMANCE
                    else -> ScamType.OTHER
                }
            }
            "personal_info" -> ScamType.PHISHING
            "urgency" -> {
                val impersonationScore = categoryScores["impersonation"] ?: 0f
                val phishingScore = categoryScores["phishing_url"] ?: 0f
                when {
                    impersonationScore > 1f && phishingScore > 0f -> ScamType.PHISHING
                    else -> ScamType.OTHER
                }
            }
            else -> ScamType.OTHER
        }
    }
}
