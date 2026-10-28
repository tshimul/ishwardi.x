package com.example.data.local

import com.example.data.model.CategoryEntity
import com.example.data.model.LocationAreaEntity
import com.example.data.model.ProviderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseInitializer {

    suspend fun seedIfNeeded(database: AppDatabase) = withContext(Dispatchers.IO) {
        val categoryDao = database.categoryDao()
        val locationDao = database.locationAreaDao()
        val providerDao = database.providerDao()

        // 1. Seed Categories if empty
        if (categoryDao.getCount() == 0) {
            val initialCategories = listOf(
                CategoryEntity("plumbing", "Plumbing", "প্লাম্বিং ও পাইপলাইন", "plumbing", 1),
                CategoryEntity("electricians", "Electricians", "ইলেকট্রিশিয়ান ও ওয়্যারিং", "electric_bolt", 2),
                CategoryEntity("internet", "Internet and Broadband", "ইন্টারনেট ও ব্রডব্যান্ড", "wifi", 3),
                CategoryEntity("electricity", "Electricity Services", "বিদ্যুৎ সেবা (নেসকো/পল্লী বিদ্যুৎ)", "bolt", 4),
                CategoryEntity("gas", "Gas Services", "গ্যাস সিলিন্ডার ও লাইন সেবা", "propane_tank", 5),
                CategoryEntity("water", "Water Services", "পানি সরবরাহ ও গভীর নলকূপ", "water_drop", 6),
                CategoryEntity("mobile", "Mobile and Telecom", "মোবাইল ও টেলিকম সার্ভিস", "smartphone", 7),
                CategoryEntity("mechanics", "Mechanics and Vehicle Repair", "গাড়ি ও মোটরসাইকেল মেকানিক", "car_repair", 8),
                CategoryEntity("electronics", "Electronics Repair", "টিভি ও ইলেকট্রনিক্স মেরামত", "tv", 9),
                CategoryEntity("it_services", "Computer and IT Services", "কম্পিউটার ও নেটওয়ার্কিং", "computer", 10),
                CategoryEntity("ac_refrigerator", "AC and Refrigerator Repair", "এসি ও ফ্রিজ সার্ভিসিং", "ac_unit", 11),
                CategoryEntity("appliances", "Home Appliances", "গৃহস্থালী ইলেকট্রিক যন্ত্রপাতি", "kitchen", 12),
                CategoryEntity("construction", "Construction and Building", "নির্মাণ ও রাজমিস্ত্রি", "construction", 13),
                CategoryEntity("carpentry", "Carpentry and Furniture", "কাঠমিস্ত্রি ও ফার্নিচার", "carpenter", 14),
                CategoryEntity("painting", "Painting", "রং মিস্ত্রি ও ডেকোরেশন", "format_paint", 15),
                CategoryEntity("cleaning", "Cleaning and Housekeeping", "পরিচ্ছন্নতা ও ক্লিনিং সার্ভিস", "cleaning_services", 16),
                CategoryEntity("transport", "Transport and Vehicle Rental", "গাড়ি/মাইক্রোবাস/পিকআপ ভাড়া", "directions_car", 17),
                CategoryEntity("emergency", "Ambulance and Emergency Services", "অ্যাম্বুলেন্স ও জরুরি সেবা", "emergency", 18),
                CategoryEntity("doctors", "Doctors and Clinics", "ডাক্তার ও চেম্বার", "medical_services", 19),
                CategoryEntity("hospitals", "Hospitals and Diagnostic Centers", "হাসপাতাল ও ডায়াগনস্টিক", "local_hospital", 20),
                CategoryEntity("pharmacies", "Pharmacies", "ফার্মেসি ও সার্বক্ষণিক ঔষধ", "local_pharmacy", 21),
                CategoryEntity("legal", "Legal and Professional Services", "আইনজীবী ও নোটারি সেবা", "gavel", 22),
                CategoryEntity("education", "Education and Coaching", "শিক্ষক, কোচিং ও টিউটর", "school", 23),
                CategoryEntity("photography", "Photography and Events", "ফটোগ্রাফি ও সাউন্ড সিস্টেম", "photo_camera", 24),
                CategoryEntity("restaurants", "Restaurants and Food Services", "রেস্তোরাঁ ও ক্যাটারিং", "restaurant", 25),
                CategoryEntity("courier", "Courier and Delivery", "কুরিয়ার ও পার্সেল সার্ভিস", "local_shipping", 26),
                CategoryEntity("government", "Government Offices and Public Utilities", "সরকারি দপ্তর ও সেবা", "account_balance", 27),
                CategoryEntity("other", "Other Services", "অন্যান্য প্রয়োজনীয় সেবা", "more_horiz", 28)
            )
            categoryDao.insertAll(initialCategories)
        }

        // 2. Seed Ishwardi Localities if empty
        if (locationDao.getCount() == 0) {
            val initialLocalities = listOf(
                LocationAreaEntity("ishwardi_sadar", "Ishwardi Sadar & Bazaar", "ঈশ্বরদী সদর ও বাজার", 1),
                LocationAreaEntity("station_road", "Station Road & Rail Gate", "রেলগেট ও স্টেশন রোড", 2),
                LocationAreaEntity("railway_colony", "Railway Colony", "রেলওয়ে লোকো কলোনি", 3),
                LocationAreaEntity("post_office_road", "Post Office Road", "পোস্ট অফিস মোড়", 4),
                LocationAreaEntity("pakshi", "Pakshi & Hardinge Bridge Area", "পাকশী ও সেতু এলাকা", 5),
                LocationAreaEntity("rooppur", "Rooppur & Project Area", "রূপপুর ও মোড়", 6),
                LocationAreaEntity("dashuria", "Dashuria Mor", "দাশুড়িয়া মোড়", 7),
                LocationAreaEntity("muladuli", "Muladuli Bazaar", "মুলাডুলি বাজার", 8),
                LocationAreaEntity("sahapur", "Sahapur", "সাহাপুর ইউনিয়ন", 9),
                LocationAreaEntity("silimpur", "Silimpur & Joynagor", "সলিমপুর ও জয়নগর", 10),
                LocationAreaEntity("lakshmikunda", "Lakshmikunda", "লক্ষ্মীকুণ্ডা", 11),
                LocationAreaEntity("sara", "Sara Union & Ghat", "সাড়াঘাট ও ইউনিয়ন", 12),
                LocationAreaEntity("airport_area", "Ishwardi Airport Area", "বিমানবন্দর এলাকা", 13),
                LocationAreaEntity("epz_area", "Ishwardi EPZ Area", "ঈশ্বরদী ইপিজেড এলাকা", 14)
            )
            locationDao.insertAll(initialLocalities)
        }

        // 3. Seed Sample Demo Provider as specifically instructed by prompt
        if (providerDao.getTotalCountDirect() == 0) {
            val demoProvider = ProviderEntity(
                id = "demo-sample-provider-1",
                name = "Noshid",
                banglaName = "নশিদ (প্লাম্বার)",
                primaryCategory = "Plumbing",
                additionalCategories = "Water Services",
                contactNumber = "555-0123",
                additionalContact = "",
                locationArea = "Ishwardi",
                specificAddress = "Station Road, Ishwardi Sadar",
                landmark = "Near Rail Station",
                chargeEstimate = "Entered by the administrator",
                description = "Demonstration record: general plumbing, pipe fitting, water line repair and sanitary maintenance.",
                whatsappNumber = "",
                facebookUrl = "",
                mapCoordinates = "24.1528,89.0664",
                workingHours = "9:00 AM - 8:00 PM",
                isVerified = false,
                isActive = true,
                isDemoData = true
            )
            providerDao.insert(demoProvider)
        }
    }
}
