package com.scamradar.app.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ScamPattern(
    val id: Int,
    val name: String,
    val description: String,
    val category: String,
    val severity: String,
    val detailedDescription: String,
    val exampleIndicators: List<String>,
    val howToSpot: List<String>
)

private val scamPatterns = listOf(
    ScamPattern(
        id = 1,
        name = "Phishing Email",
        description = "Fraudulent emails mimicking trusted organizations",
        category = "Phishing",
        severity = "High",
        detailedDescription = "Phishing emails are crafted to look like legitimate communications from banks, payment processors, or well-known companies. They typically contain urgent language pressuring you to click a link or download an attachment. The links lead to fake login pages designed to steal your credentials, or the attachments install malware on your device.",
        exampleIndicators = listOf(
            "Urgent subject lines like 'Account Suspended' or 'Immediate Action Required'",
            "Generic greetings like 'Dear Customer' instead of your name",
            "Misspelled domain names (e.g., paypa1.com instead of paypal.com)",
            "Links that don't match the sender's claimed organization",
            "Requests for personal information via email"
        ),
        howToSpot = listOf(
            "Check the sender's actual email address, not just the display name",
            "Hover over links without clicking to see the real URL",
            "Look for grammatical errors and unprofessional formatting",
            "Verify claims by contacting the organization directly through official channels",
            "Be suspicious of any email creating a false sense of urgency"
        )
    ),
    ScamPattern(
        id = 2,
        name = "USPS/FedEx Smishing",
        description = "Fake package delivery text messages",
        category = "Delivery",
        severity = "High",
        detailedDescription = "Smishing (SMS phishing) scams impersonate delivery services like USPS, FedEx, or UPS. These text messages claim you have a package that needs attention, a delivery that failed, or customs fees to pay. The included links lead to phishing sites that steal personal and financial information.",
        exampleIndicators = listOf(
            "Text from an unknown number claiming to be USPS or FedEx",
            "Links to domains like 'usps-package-track.com' instead of usps.com",
            "Claims of an 'undeliverable package' requiring address confirmation",
            "Requests for payment of small customs or shipping fees",
            "Short, urgent messages with suspicious shortened URLs"
        ),
        howToSpot = listOf(
            "Legitimate delivery services rarely send unsolicited text messages",
            "Check tracking numbers directly on the official carrier website",
            "Never click links in unexpected delivery text messages",
            "If you aren't expecting a package, it's likely a scam",
            "Real delivery notifications come from official short codes, not random phone numbers"
        )
    ),
    ScamPattern(
        id = 3,
        name = "IRS Impersonation",
        description = "Fake IRS calls demanding immediate payment",
        category = "Impersonation",
        severity = "Critical",
        detailedDescription = "Scammers impersonate IRS agents and demand immediate payment for alleged back taxes, penalties, or fines. They use threatening language, claiming you'll be arrested, deported, or have your assets seized if you don't pay right away. They often demand payment via gift cards, wire transfers, or cryptocurrency—methods the real IRS would never use.",
        exampleIndicators = listOf(
            "Threats of arrest or legal action for unpaid taxes",
            "Demands for payment via gift cards, wire transfer, or crypto",
            "Caller ID spoofing that shows 'IRS' or a Washington D.C. number",
            "Refusal to allow you to call back or verify through official channels",
            "Claims that you'll lose your driver's license or passport"
        ),
        howToSpot = listOf(
            "The IRS never calls to demand immediate payment",
            "The IRS will never ask for credit/debit card numbers over the phone",
            "The IRS always sends written notices by mail first",
            "Legitimate IRS agents provide their badge number and you can verify it",
            "The IRS does not accept gift cards as payment"
        )
    ),
    ScamPattern(
        id = 4,
        name = "AI Voice Cloning",
        description = "Synthesized voice calls impersonating family members",
        category = "Voice",
        severity = "Critical",
        detailedDescription = "Using AI-powered voice cloning technology, scammers replicate the voice of someone you know—often a family member—and call you in distress. The cloned voice claims to be in an emergency situation (car accident, arrest, kidnapping) and urgently needs money. The quality of AI voice cloning has improved dramatically, making it nearly indistinguishable from the real person.",
        exampleIndicators = listOf(
            "A call from an unknown number sounding exactly like a loved one",
            "Claims of being in a legal or medical emergency",
            "Requests for immediate wire transfer or bail money",
            "Short calls with poor audio quality or unusual pauses",
            "Instructions not to contact other family members"
        ),
        howToSpot = listOf(
            "Ask a personal question only the real person would know",
            "Hang up and call the person's actual phone number directly",
            "Be suspicious if the caller asks you not to tell anyone",
            "Notice if the call feels unusually rushed or the voice sounds slightly off",
            "Verify the story with another family member before sending money"
        )
    ),
    ScamPattern(
        id = 5,
        name = "Romance Scam",
        description = "Fake online relationships built to steal money",
        category = "Impersonation",
        severity = "High",
        detailedDescription = "Romance scammers create fake profiles on dating apps and social media, building emotional connections over weeks or months. Once trust is established, they fabricate emergencies, investment opportunities, or travel expenses and ask for money. They always avoid meeting in person and often claim to be working overseas or in the military.",
        exampleIndicators = listOf(
            "Quick declarations of love before meeting in person",
            "Claims of being stationed overseas or working on oil rigs",
            "Elaborate stories about needing money for medical emergencies",
            "Requests for money via wire transfer, gift cards, or cryptocurrency",
            "Refusal to video chat or always having an excuse to avoid it"
        ),
        howToSpot = listOf(
            "Be cautious if someone professes love very quickly",
            "Reverse image search their profile photos",
            "Never send money to someone you haven't met in person",
            "Insist on a live video call early in the relationship",
            "Watch for inconsistencies in their stories or background"
        )
    ),
    ScamPattern(
        id = 6,
        name = "Crypto Investment",
        description = "Fake cryptocurrency investment opportunities",
        category = "Investment",
        severity = "Critical",
        detailedDescription = "Crypto investment scams promise guaranteed high returns on cryptocurrency investments. They may use fake trading platforms, impersonate successful traders on social media, or run Ponzi schemes disguised as crypto funds. Victims are shown fake dashboards showing enormous profits, but when they try to withdraw, they're told to pay additional fees or taxes.",
        exampleIndicators = listOf(
            "Guaranteed returns of 100% or more with 'zero risk'",
            "Celebrity endorsements that are fabricated or taken out of context",
            "Pressure to invest immediately before the 'opportunity closes'",
            "Fake trading dashboards showing inflated profits",
            "Required to pay 'withdrawal fees' or 'taxes' before accessing your funds"
        ),
        howToSpot = listOf(
            "No legitimate investment guarantees returns, especially in crypto",
            "Verify the trading platform is registered with financial regulators",
            "Be wary of investments promoted through social media ads or DMs",
            "If you can't withdraw your money, it's almost certainly a scam",
            "Research the company and look for independent reviews"
        )
    ),
    ScamPattern(
        id = 7,
        name = "Tech Support",
        description = "Fake pop-ups and calls claiming your device is infected",
        category = "Phishing",
        severity = "High",
        detailedDescription = "Tech support scams start with alarming pop-up messages or cold calls claiming your computer is infected with viruses or has been hacked. The scammers impersonate Microsoft, Apple, or other tech companies and convince you to grant remote access to your device. Once connected, they install malware, steal personal data, or charge you for unnecessary 'repairs.'",
        exampleIndicators = listOf(
            "Browser pop-ups claiming your computer is infected with multiple viruses",
            "Phone calls from people claiming to be from Microsoft or Apple support",
            "Requests to download remote access software like AnyDesk or TeamViewer",
            "Pop-ups with loud alarm sounds and flashing red warnings",
            "Demands for payment via gift cards for 'technical support services'"
        ),
        howToSpot = listOf(
            "Microsoft and Apple will never cold-call you about a virus",
            "Legitimate error messages don't include phone numbers to call",
            "Never grant remote access to an unsolicited caller",
            "Close the browser entirely if you see a suspicious pop-up",
            "Real tech companies don't ask for payment via gift cards"
        )
    ),
    ScamPattern(
        id = 8,
        name = "Job Offer",
        description = "Fake employment offers requiring upfront payments",
        category = "Impersonation",
        severity = "Medium",
        detailedDescription = "Job scammers post fake listings on job boards or send unsolicited offers via email and text. They may ask for personal information for 'background checks,' require payment for training materials or equipment, or involve you in money laundering through fake check schemes. Remote work and work-from-home scams are increasingly common.",
        exampleIndicators = listOf(
            "Job offers for positions you never applied for",
            "Requests for personal information like SSN before an interview",
            "Requirements to pay for training, equipment, or background checks upfront",
            "Job listings with vague descriptions and unusually high pay",
            "Receiving a check and being asked to forward a portion to someone else"
        ),
        howToSpot = listOf(
            "Legitimate employers never ask you to pay to work for them",
            "Research the company and verify the job posting on their official site",
            "Be suspicious of jobs offering high pay for minimal work",
            "Never deposit a check from someone you don't know and send money back",
            "Verify the recruiter's email address matches the company domain"
        )
    ),
    ScamPattern(
        id = 9,
        name = "Lottery/Sweepstakes",
        description = "Fake notifications claiming you won a prize",
        category = "Phishing",
        severity = "Medium",
        detailedDescription = "You receive a notification—by email, phone, mail, or text—claiming you've won a lottery, sweepstakes, or prize draw you never entered. To claim your winnings, you're asked to pay taxes, fees, or customs charges upfront. These scams often use the names of real organizations like Publishers Clearing House or well-known lotteries to appear legitimate.",
        exampleIndicators = listOf(
            "Notifications about winning a lottery you never entered",
            "Requests for upfront fees to claim your prize",
            "Demands for bank account information to deposit winnings",
            "Emails from 'officials' at foreign lotteries",
            "Pressure to keep your 'winning' confidential"
        ),
        howToSpot = listOf(
            "You cannot win a lottery you didn't enter",
            "Legitimate lotteries never require upfront fees to claim prizes",
            "Real prize organizations contact winners in person, not via email",
            "Never give bank account details to claim a prize",
            "If it sounds too good to be true, it almost certainly is"
        )
    ),
    ScamPattern(
        id = 10,
        name = "Netflix/Streaming Billing",
        description = "Fake subscription billing alerts",
        category = "Phishing",
        severity = "Medium",
        detailedDescription = "Scammers send emails or text messages that appear to be from Netflix, Spotify, Amazon Prime, or other streaming services. The messages claim your subscription is suspended, your payment failed, or there's suspicious activity on your account. The included links lead to fake login pages that capture your credentials and payment card details.",
        exampleIndicators = listOf(
            "Emails claiming your Netflix or streaming account is suspended",
            "Links leading to pages asking for credit card information",
            "Poor grammar or formatting that doesn't match the real service",
            "Sender email addresses that don't match the official domain",
            "Threats to cancel your account if you don't act immediately"
        ),
        howToSpot = listOf(
            "Log into your streaming accounts directly through the official app or website",
            "Check the sender's email address carefully",
            "Real services don't ask for payment info via email links",
            "If concerned, navigate to the service's site manually, not through email links",
            "Look for your name in the email—generic greetings are a red flag"
        )
    ),
    ScamPattern(
        id = 11,
        name = "Family Emergency",
        description = "Calls or messages claiming a family member is in danger",
        category = "Voice",
        severity = "Critical",
        detailedDescription = "Scammers call or text claiming a family member—often a grandchild, child, or spouse—is in trouble. They might say the person was in an accident, arrested, kidnapped, or hospitalized and urgently needs money for bail, medical bills, or legal fees. The scammer may put another person on the line who sounds like the family member or use AI voice cloning.",
        exampleIndicators = listOf(
            "A call from an unknown number about a family emergency",
            "Requests for money via wire transfer, gift cards, or cryptocurrency",
            "Instructions not to contact other family members",
            "The caller knows personal details about your family from social media",
            "Extreme urgency and pressure to act immediately"
        ),
        howToSpot = listOf(
            "Hang up and call your family member directly on their known number",
            "Ask the caller a question only the real family member would answer",
            "Contact other family members to verify the story",
            "Never send money based on an unexpected phone call",
            "Be cautious about what personal information you share on social media"
        )
    ),
    ScamPattern(
        id = 12,
        name = "Identity Theft",
        description = "Stealing personal information to commit fraud",
        category = "Phishing",
        severity = "Critical",
        detailedDescription = "Identity thieves collect your personal information—Social Security number, bank details, date of birth—through data breaches, phishing, skimming devices, or social engineering. They use this information to open credit accounts, file tax returns, get medical care, or commit crimes in your name. The damage can take months or years to fully resolve.",
        exampleIndicators = listOf(
            "Unfamiliar charges or accounts on your credit report",
            "Bills or collection notices for services you didn't use",
            "Tax return rejection because someone already filed using your SSN",
            "Unexpected login notifications from your financial accounts",
            "Mail that stops arriving (indicating address change fraud)"
        ),
        howToSpot = listOf(
            "Monitor your credit reports regularly for unauthorized accounts",
            "Set up alerts on all financial accounts for unusual activity",
            "Shred documents containing personal information before discarding",
            "Use strong, unique passwords and enable two-factor authentication",
            "Freeze your credit with all three bureaus if you suspect a breach"
        )
    )
)

private val categories = listOf("All", "Phishing", "Voice", "Investment", "Impersonation", "Delivery")

private val severityColors = mapOf(
    "Low" to Color(0xFF4CAF50),
    "Medium" to Color(0xFFFF9800),
    "High" to Color(0xFFFF5722),
    "Critical" to Color(0xFFD32F2F)
)

private val categoryColors = mapOf(
    "Phishing" to Color(0xFF42A5F5),
    "Voice" to Color(0xFFAB47BC),
    "Investment" to Color(0xFF66BB6A),
    "Impersonation" to Color(0xFFFFA726),
    "Delivery" to Color(0xFFEF5350)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    initialPatternId: Int? = null
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPattern by remember { mutableStateOf<ScamPattern?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(initialPatternId) {
        selectedPattern = scamPatterns.firstOrNull { it.id == initialPatternId }
    }

    val filteredPatterns = remember(selectedCategory, searchQuery) {
        val query = searchQuery.trim()
        scamPatterns
            .filter { selectedCategory == "All" || it.category == selectedCategory }
            .filter { pattern ->
                query.isBlank() ||
                    pattern.name.contains(query, ignoreCase = true) ||
                    pattern.description.contains(query, ignoreCase = true) ||
                    pattern.category.contains(query, ignoreCase = true) ||
                    pattern.severity.contains(query, ignoreCase = true) ||
                    pattern.detailedDescription.contains(query, ignoreCase = true) ||
                    pattern.exampleIndicators.any { it.contains(query, ignoreCase = true) } ||
                    pattern.howToSpot.any { it.contains(query, ignoreCase = true) }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Scam Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            placeholder = { Text("Search scams") },
            shape = RoundedCornerShape(16.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (filteredPatterns.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No scams found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Try a different keyword or category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPatterns, key = { it.id }) { pattern ->
                    ScamPatternCard(
                        pattern = pattern,
                        onClick = { selectedPattern = pattern }
                    )
                }
            }
        }
    }

    selectedPattern?.let { pattern ->
        ModalBottomSheet(
            onDismissRequest = { selectedPattern = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(categoryColors[pattern.category] ?: Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pattern.name.first().toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pattern.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(severityColors[pattern.severity] ?: Color.Gray)
                            )
                            Text(
                                text = "${pattern.severity} · ${pattern.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = pattern.detailedDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "How to Spot It",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                pattern.howToSpot.forEachIndexed { index, tip ->
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Example Indicators",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                pattern.exampleIndicators.forEachIndexed { index, indicator ->
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = indicator,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScamPatternCard(
    pattern: ScamPattern,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColors[pattern.category] ?: Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pattern.name.first().toString(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(severityColors[pattern.severity] ?: Color.Gray)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pattern.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pattern.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
