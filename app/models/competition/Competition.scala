package models.competition

import java.sql.Timestamp
import java.util.Date

import db.Named
import db.access.ModelAccess
import db.impl.CompetitionTable
import db.impl.model.OreModel
import db.impl.model.common.Describable
import db.impl.table.ModelKeys._
import form.project.competition.{CompetitionCreateForm, CompetitionSaveForm}
import models.user.User
import ore.user.UserOwned
import util.StringUtils.{localDateTime2timestamp, nullIfEmpty}

import scala.concurrent.duration._

/**
  * Represents a [[models.project.Project]] competition.
  *
  * @param id                   Unique ID
  * @param createdAt            Instant of creation
  * @param userId               Owner ID
  * @param name                 Competition name
  * @param _description         Competition description
  * @param _startDate           Date when the competition begins
  * @param _endDate             Date when the competition ends
  * @param _timeZone            Time zone of competition
  * @param _isVotingEnabled     True if project voting is enabled
  * @param _isStaffVotingOnly   True if only staff members can vote
  * @param _shouldShowVoteCount True if the vote count should be displayed
  * @param isSpongeOnly         True if only Sponge plugins are permitted in the competition
  * @param _isSourceRequired    True if source-code is required for entry to the competition
  * @param _defaultVotes        Default amount of votes a user has
  * @param _staffVotes          The amount of votes staff-members have
  * @param _allowedEntries      The amount of entries a user may submit
  * @param _maxEntryTotal       The total amount of projects allowed in the competition
  */
case class Competition(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       override val name: String,
                       private var _description: Option[String] = None,
                       private var _startDate: Timestamp,
                       private var _endDate: Timestamp,
                       private var _timeZone: String,
                       private var _isVotingEnabled: Boolean = true,
                       private var _isStaffVotingOnly: Boolean = false,
                       private var _shouldShowVoteCount: Boolean = true,
                       isSpongeOnly: Boolean,
                       private var _isSourceRequired: Boolean = false,
                       private var _defaultVotes: Int = 1,
                       private var _staffVotes: Int = 1,
                       private var _allowedEntries: Int = 1,
                       private var _maxEntryTotal: Int = -1)
                       extends OreModel(id, createdAt) with Named with Describable with UserOwned {

  override type M = Competition
  override type T = CompetitionTable

  def this(user: User, formData: CompetitionCreateForm) = this(
    userId = user.id.get,
    name = formData.name.trim,
    _description = Option(formData.description.map(s => nullIfEmpty(s.trim)).orNull),
    _startDate = localDateTime2timestamp(formData.startDate, formData.timeZoneId),
    _endDate = localDateTime2timestamp(formData.endDate, formData.timeZoneId),
    _timeZone = formData.timeZoneId,
    _isVotingEnabled = formData.isVotingEnabled,
    _isStaffVotingOnly = formData.isStaffVotingOnly,
    _shouldShowVoteCount = formData.shouldShowVoteCount,
    isSpongeOnly = formData.isSpongeOnly,
    _isSourceRequired = formData.isSourceRequired,
    _defaultVotes = formData.defaultVotes,
    _staffVotes = formData.staffVotes,
    _allowedEntries = formData.allowedEntries,
    _maxEntryTotal = formData.maxEntryTotal
  )

  /**
    * Returns this competition's [[CompetitionEntry]]s.
    *
    * @return Competition entries
    */
  def entries: ModelAccess[CompetitionEntry]
  = this.schema.getChildren[CompetitionEntry](classOf[CompetitionEntry], this)

  /**
    * Saves this competition from the submitted form data.
    *
    * @param formData Submitted form data
    */
  def save(formData: CompetitionSaveForm) = {
    this.startDate = localDateTime2timestamp(formData.startDate, formData.timeZoneId)
    this.endDate = localDateTime2timestamp(formData.endDate, formData.timeZoneId)
    this.setVotingEnabled(formData.isVotingEnabled)
    this.setStaffVotingOnly(formData.isStaffVotingOnly)
    this.setShouldShowVoteCount(formData.shouldShowVoteCount)
    this.setSourceRequired(formData.isSourceRequired)
    this.defaultVotes = formData.defaultVotes
    this.staffVotes = formData.staffVotes
    this.allowedEntries = formData.allowedEntries
    this.maxEntryTotal = formData.maxEntryTotal
  }

  /**
    * Returns the competition description.
    *
    * @return Competition description
    */
  override def description: Option[String] = this._description

  /**
    * Sets the competition description.
    *
    * @param description Competition description
    */
  def description_=(description: String) = {
    this._description = Option(description)
    if (isDefined) update(Description)
  }

  /**
    * Returns the date when the competition begins.
    *
    * @return Start date
    */
  def startDate: Timestamp = this._startDate

  /**
    * Sets the date when the competition begins.
    *
    * @param startDate Start date
    */
  def startDate_=(startDate: Timestamp) = {
    this._startDate = startDate
    if (isDefined) update(StartDate)
  }

  /**
    * Returns the date when the competition ends.
    *
    * @return End date
    */
  def endDate: Timestamp = this._endDate

  /**
    * Sets the date when the comeptition ends.
    *
    * @param endDate End date
    */
  def endDate_=(endDate: Timestamp) = {
    this._endDate = endDate
    if (isDefined) update(EndDate)
  }

  /**
    * Returns the ZoneId of the competition.
    *
    * @return ZoneId of competition
    */
  def timeZone: String = this._timeZone

  /**
    * Sets the ZoneId of the competition.
    *
    * @param timeZone ZoneId
    */
  def timeZone_=(timeZone: String) = {
    this._timeZone = timeZone
    if (isDefined) update(TimeZone)
  }

  /**
    * Returns the amount of time remaining in the competition.
    *
    * @return Time remaining in competition
    */
  def timeRemaining: Duration = (this.endDate.getTime - new Date().getTime).millis

  /**
    * Returns true if the competition has voting enabled.
    *
    * @return True if voting is enabled
    */
  def isVotingEnabled: Boolean = this._isVotingEnabled

  /**
    * Sets whether voting is enabled in this comeptition.
    *
    * @param votingEnabled True if voting is enabled
    */
  def setVotingEnabled(votingEnabled: Boolean) = {
    this._isVotingEnabled = votingEnabled
    if (isDefined) update(IsVotingEnabled)
  }

  /**
    * Returns true if only staff members can vote in the competition.
    *
    * @return True if only staff can vote
    */
  def isStaffVotingOnly: Boolean = this._isStaffVotingOnly

  /**
    * Sets whether only staff members can vote in the competition.
    *
    * @param staffVotingOnly True if only staff can vote
    */
  def setStaffVotingOnly(staffVotingOnly: Boolean) = {
    this._isStaffVotingOnly = staffVotingOnly
    if (isDefined) update(IsStaffVotingOnly)
  }

  /**
    * Returns true if vote counters are displayed for this competition.
    *
    * @return True if vote counters are displayed
    */
  def shouldShowVoteCount: Boolean = this._shouldShowVoteCount

  /**
    * Sets whether vote counters should be displayed for this competition.
    *
    * @param shouldShowVoteCount True if vote counters should be displayed
    */
  def setShouldShowVoteCount(shouldShowVoteCount: Boolean) = {
    this._shouldShowVoteCount = shouldShowVoteCount
    if (isDefined) update(ShouldShowVoteCount)
  }

  /**
    * Returns true if source-code is required for entry to this competition.
    *
    * @return True if source-code is required
    */
  def isSourceRequired: Boolean = this._isSourceRequired

  /**
    * Sets whether source-code is required for entry to this competition.
    *
    * @param sourceRequired True if source-code is required
    */
  def setSourceRequired(sourceRequired: Boolean) = {
    this._isSourceRequired = sourceRequired
    if (isDefined) update(IsSourceRequired)
  }

  /**
    * Returns the default amount of votes a user has.
    *
    * @return Default amount of votes
    */
  def defaultVotes: Int = this._defaultVotes

  /**
    * Sets the default amount of votes a user has.
    *
    * @param defaultVotes Default amount of votes
    */
  def defaultVotes_=(defaultVotes: Int) = {
    this._defaultVotes = defaultVotes
    if (isDefined) update(DefaultVotes)
  }

  /**
    * Returns the amount of votes staff members have.
    *
    * @return Staff member votes
    */
  def staffVotes: Int = this._staffVotes

  /**
    * Sets the amount of votes staff members have.
    *
    * @param staffVotes Staff member votes
    */
  def staffVotes_=(staffVotes: Int) = {
    this._staffVotes = staffVotes
    if (isDefined) update(StaffVotes)
  }

  /**
    * Returns the amount of entries a user may submit.
    *
    * @return Amount of entries
    */
  def allowedEntries: Int = this._allowedEntries

  /**
    * Sets the amount of entries a user may submit.
    *
    * @param allowedEntries Amount of entries
    */
  def allowedEntries_=(allowedEntries: Int) = {
    this._allowedEntries = allowedEntries
    if (isDefined) update(AllowedEntries)
  }

  /**
    * Returns the maximum amount of entries in the competition.
    *
    * @return Maximum amount of entries
    */
  def maxEntryTotal: Int = this._maxEntryTotal

  /**
    * Sets the maximum amount of entries in the competition.
    *
    * @param maxEntryTotal Maximum amount of entries
    */
  def maxEntryTotal_=(maxEntryTotal: Int) = {
    this._maxEntryTotal = maxEntryTotal
    if (isDefined) update(MaxEntryTotal)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
