package cz.applifting.humansis.db.dao

import androidx.room.*
import cz.applifting.humansis.model.ReferralType
import cz.applifting.humansis.model.db.BeneficiaryLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {

    // Note: Room does not seem to support SQL queries for just checking if something exists
    @Query("SELECT * FROM beneficiaries WHERE edited = 1 LIMIT 1")
    fun arePendingChanges(): Flow<List<BeneficiaryLocal>>

    @Query("SELECT * FROM beneficiaries")
    fun getAllBeneficiaries(): Flow<List<BeneficiaryLocal>>

    @Query("SELECT * FROM beneficiaries WHERE distributed = 1 AND edited = 1")
    suspend fun getAssignedBeneficiariesSuspend(): List<BeneficiaryLocal>

    @Query("SELECT * FROM beneficiaries where assistanceId = :assistanceId")
    fun getByDistribution(assistanceId: Int): Flow<List<BeneficiaryLocal>>

    @Query("SELECT * FROM beneficiaries where assistanceId = :assistanceId")
    suspend fun getByDistributionSuspend(assistanceId: Int): List<BeneficiaryLocal>

    @Query("SELECT * FROM beneficiaries where id = :beneficiaryId")
    suspend fun findById(beneficiaryId: Int): BeneficiaryLocal?

    @Query("SELECT * FROM beneficiaries where id = :beneficiaryId")
    fun findByIdFlow(beneficiaryId: Int): Flow<BeneficiaryLocal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(beneficiariesLocal: List<BeneficiaryLocal>)

    @Update
    suspend fun update(beneficiaryLocal: BeneficiaryLocal)

    @Query("DELETE FROM beneficiaries WHERE assistanceId = :assistanceId")
    suspend fun deleteByDistribution(assistanceId: Int)

    @Query("DELETE FROM beneficiaries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(id) FROM beneficiaries WHERE assistanceId = :assistanceId AND distributed = 1")
    suspend fun countReachedBeneficiaries(assistanceId: Int): Int

    @Query("SELECT qrBooklets FROM beneficiaries")
    suspend fun getAllBooklets(): List<String>?

    @Query("UPDATE beneficiaries SET referralType = :referralType, referralNote = :referralNote where beneficiaryId = :beneficiaryId")
    suspend fun updateReferralOfMultiple(beneficiaryId: Int, referralType: ReferralType?, referralNote: String?)

    @Query("SELECT COUNT(id) from beneficiaries WHERE beneficiaryId = :beneficiaryId AND edited = 1")
    suspend fun countDuplicateAssignedBeneficiaries(beneficiaryId: Int): Int

    @Query("SELECT * from beneficiaries WHERE (referralType IS NOT originalReferralType OR referralNote IS NOT originalReferralNote) GROUP BY beneficiaryId")
    suspend fun getAllReferralChanges(): List<BeneficiaryLocal>
}