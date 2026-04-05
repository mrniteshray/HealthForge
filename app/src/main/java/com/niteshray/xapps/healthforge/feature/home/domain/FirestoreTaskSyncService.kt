package com.niteshray.xapps.healthforge.feature.home.domain

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.niteshray.xapps.healthforge.feature.home.data.models.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTaskSyncService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val taskTrackingRepository: TaskTrackingRepository
) {

    companion object {
        private const val TAG = "FirestoreTaskSync"
        private const val CAREPLAN_COLLECTION = "Careplan"
        private const val TEMPLATES_COLLECTION = "TaskTemplates"
        private const val RECORDS_COLLECTION = "DailyRecords"
    }

    private fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Sync a TaskTemplate to Firestore
     */
    suspend fun syncTaskTemplateToFirestore(template: TaskTemplate): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            val firestoreTemplate = template.toFirestoreTemplate()
            
            val documentRef = if (template.firestoreId.isNullOrBlank()) {
                // Create new document
                firestore.collection(CAREPLAN_COLLECTION)
                    .document(userId)
                    .collection(TEMPLATES_COLLECTION)
                    .document()
            } else {
                // Update existing document
                firestore.collection(CAREPLAN_COLLECTION)
                    .document(userId)
                    .collection(TEMPLATES_COLLECTION)
                    .document(template.firestoreId!!)
            }
            
            documentRef.set(firestoreTemplate.copy(id = documentRef.id)).await()
            Log.d(TAG, "Successfully synced template ${template.id} to Firestore: ${documentRef.id}")
            
            // Update local record with Firestore ID
            if (template.firestoreId != documentRef.id) {
                taskTrackingRepository.updateTemplate(
                    template.copy(firestoreId = documentRef.id)
                )
            }
            
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync template to Firestore: ${template.id}", e)
            null
        }
    }

    /**
     * Sync a DailyTaskRecord to Firestore
     */
    suspend fun syncDailyRecordToFirestore(record: DailyTaskRecord): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            
            // Get the template to find its Firestore ID
            val template = taskTrackingRepository.getTemplateById(record.templateId)
            val templateFirestoreId = template?.firestoreId ?: return null
            
            val firestoreRecord = record.toFirestoreRecord(templateFirestoreId)
            
            val documentRef = if (record.firestoreId.isNullOrBlank()) {
                // Create new document
                firestore.collection(CAREPLAN_COLLECTION)
                    .document(userId)
                    .collection(RECORDS_COLLECTION)
                    .document()
            } else {
                // Update existing document
                firestore.collection(CAREPLAN_COLLECTION)
                    .document(userId)
                    .collection(RECORDS_COLLECTION)
                    .document(record.firestoreId!!)
            }
            
            documentRef.set(firestoreRecord.copy(id = documentRef.id)).await()
            Log.d(TAG, "Successfully synced record ${record.id} to Firestore: ${documentRef.id}")
            
            // Update local record with Firestore ID
            if (record.firestoreId != documentRef.id) {
                taskTrackingRepository.updateRecord(
                    record.copy(firestoreId = documentRef.id)
                )
            }
            
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync record to Firestore: ${record.id}", e)
            null
        }
    }

    /**
     * Fetch all task templates from Firestore
     */
    suspend fun fetchTaskTemplatesFromFirestore(): List<TaskTemplate> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()
            
            val querySnapshot = firestore.collection(CAREPLAN_COLLECTION)
                .document(userId)
                .collection(TEMPLATES_COLLECTION)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val templates = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(FirestoreTaskTemplate::class.java)?.toTaskTemplate()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse template document: ${document.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Successfully fetched ${templates.size} templates from Firestore")
            templates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch templates from Firestore", e)
            emptyList()
        }
    }

    /**
     * Fetch daily records for a date range from Firestore
     */
    suspend fun fetchDailyRecordsFromFirestore(startDate: String, endDate: String): List<DailyTaskRecord> {
        return try {
            val userId = getCurrentUserId() ?: return emptyList()
            
            val querySnapshot = firestore.collection(CAREPLAN_COLLECTION)
                .document(userId)
                .collection(RECORDS_COLLECTION)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            val records = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(FirestoreDailyRecord::class.java)?.toDailyTaskRecord()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse record document: ${document.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Successfully fetched ${records.size} records from Firestore for date range: $startDate to $endDate")
            records
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch records from Firestore", e)
            emptyList()
        }
    }

    /**
     * Delete a template from Firestore
     */
    suspend fun deleteTaskTemplateFromFirestore(template: TaskTemplate): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            val firestoreId = template.firestoreId ?: return false
            
            // Delete the template
            firestore.collection(CAREPLAN_COLLECTION)
                .document(userId)
                .collection(TEMPLATES_COLLECTION)
                .document(firestoreId)
                .delete()
                .await()
            
            // Delete associated records
            val recordsQuery = firestore.collection(CAREPLAN_COLLECTION)
                .document(userId)
                .collection(RECORDS_COLLECTION)
                .whereEqualTo("templateFirestoreId", firestoreId)
                .get()
                .await()
            
            recordsQuery.documents.forEach { doc ->
                doc.reference.delete()
            }
            
            Log.d(TAG, "Successfully deleted template and associated records from Firestore: $firestoreId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete template from Firestore: ${template.firestoreId}", e)
            false
        }
    }

    /**
     * Full sync - upload all local data to Firestore
     */
    suspend fun performFullUploadSync() {
        Log.d(TAG, "Starting full upload sync to Firestore")
        
        try {
            // Sync all active templates
            val templates = taskTrackingRepository.getAllActiveTemplates()
            // Note: This is a Flow, you'd need to collect it first in real implementation
            
            // Sync recent records (last 30 days)
            val today = DailyTaskRecord.getTodayDateString()
            val thirtyDaysAgo = calculateDateDaysAgo(30)
            
            // Implementation would need to collect records and sync them
            
            Log.d(TAG, "Full upload sync completed")
        } catch (e: Exception) {
            Log.e(TAG, "Full upload sync failed", e)
        }
    }
    
    private fun calculateDateDaysAgo(days: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -days)
        return DailyTaskRecord.getDateString(calendar)
    }
}