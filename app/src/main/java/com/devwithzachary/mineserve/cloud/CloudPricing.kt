package com.devwithzachary.mineserve.cloud

import java.util.Locale

data class LocalizedCloudPricing(
    val currencyCode: String,
    val free: String,
    val plusMonthly: String,
    val plusYearly: String,
    val proMonthly: String,
    val proYearly: String
) {
    val isEstimatedLocal: Boolean get() = currencyCode != "GBP"
}

object CloudPricing {
    private val gbp = LocalizedCloudPricing("GBP", "£0", "£2.99", "£29.99", "£6.99", "£69.99")
    private val usd = LocalizedCloudPricing("USD", "$0", "$3.99", "$39.99", "$8.99", "$89.99")
    private val eur = LocalizedCloudPricing("EUR", "€0", "€3.49", "€34.99", "€7.99", "€79.99")
    private val cad = LocalizedCloudPricing("CAD", "CA$0", "CA$5.49", "CA$54.99", "CA$12.49", "CA$124.99")
    private val aud = LocalizedCloudPricing("AUD", "A$0", "A$5.99", "A$59.99", "A$13.99", "A$139.99")
    private val nzd = LocalizedCloudPricing("NZD", "NZ$0", "NZ$6.49", "NZ$64.99", "NZ$14.99", "NZ$149.99")
    private val brl = LocalizedCloudPricing("BRL", "R$0", "R$21.90", "R$219", "R$49.90", "R$499")
    private val inr = LocalizedCloudPricing("INR", "₹0", "₹319", "₹3,190", "₹749", "₹7,490")
    private val jpy = LocalizedCloudPricing("JPY", "¥0", "¥590", "¥5,900", "¥1,390", "¥13,900")
    private val pln = LocalizedCloudPricing("PLN", "0 zł", "14.99 zł", "149.99 zł", "34.99 zł", "349.99 zł")
    private val sek = LocalizedCloudPricing("SEK", "0 kr", "39 kr", "399 kr", "89 kr", "899 kr")

    private val euroCountries = setOf(
        "AT", "BE", "CY", "DE", "EE", "ES", "FI", "FR", "GR", "HR",
        "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PT", "SI", "SK"
    )

    fun current(locale: Locale = Locale.getDefault()): LocalizedCloudPricing {
        return when (locale.country.uppercase(Locale.US)) {
            "GB", "UK" -> gbp
            "US" -> usd
            "CA" -> cad
            "AU" -> aud
            "NZ" -> nzd
            "BR" -> brl
            "IN" -> inr
            "JP" -> jpy
            "PL" -> pln
            "SE" -> sek
            in euroCountries -> eur
            else -> gbp
        }
    }
}

