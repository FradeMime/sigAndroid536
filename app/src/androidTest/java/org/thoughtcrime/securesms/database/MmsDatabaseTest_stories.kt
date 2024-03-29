package org.thoughtcrime.securesms.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.thoughtcrime.securesms.database.model.DistributionListId
import org.thoughtcrime.securesms.database.model.StoryType
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.mms.IncomingMediaMessage
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.whispersystems.signalservice.api.push.ACI
import org.whispersystems.signalservice.api.push.PNI
import org.whispersystems.signalservice.api.push.ServiceId
import java.util.UUID

@Suppress("ClassName")
@RunWith(AndroidJUnit4::class)
class MmsDatabaseTest_stories {

  private lateinit var mms: MmsDatabase

  private val localAci = ACI.from(UUID.randomUUID())
  private val localPni = PNI.from(UUID.randomUUID())

  private lateinit var myStory: Recipient
  private lateinit var recipients: List<RecipientId>

  @Before
  fun setUp() {
    mms = SignalDatabase.mms

    mms.deleteAllThreads()

    SignalStore.account().setAci(localAci)
    SignalStore.account().setPni(localPni)

    myStory = Recipient.resolved(SignalDatabase.recipients.getOrInsertFromDistributionListId(DistributionListId.MY_STORY))
    recipients = (0 until 5).map { SignalDatabase.recipients.getOrInsertFromServiceId(ServiceId.from(UUID.randomUUID())) }
  }

  @Test
  fun givenNoStories_whenIGetOrderedStoryRecipientsAndIds_thenIExpectAnEmptyList() {
    // WHEN
    val result = mms.orderedStoryRecipientsAndIds

    // THEN
    assertEquals(0, result.size)
  }

  @Test
  fun givenOneOutgoingAndOneIncomingStory_whenIGetOrderedStoryRecipientsAndIds_thenIExpectIncomingThenOutgoing() {
    // GIVEN
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(myStory)
    val sender = recipients[0]

    MmsHelper.insert(
      recipient = myStory,
      sentTimeMillis = 1,
      storyType = StoryType.STORY_WITH_REPLIES,
      threadId = threadId
    )

    MmsHelper.insert(
      IncomingMediaMessage(
        from = sender,
        sentTimeMillis = 2,
        serverTimeMillis = 2,
        receivedTimeMillis = 2,
        storyType = StoryType.STORY_WITH_REPLIES
      ),
      -1L
    )

    // WHEN
    val result = mms.orderedStoryRecipientsAndIds

    // THEN
    assertEquals(listOf(sender.toLong(), myStory.id.toLong()), result.map { it.recipientId.toLong() })
  }

  @Test
  fun givenAStory_whenISetIncomingStoryMessageViewed_thenIExpectASetReceiptTimestamp() {
    // GIVEN
    val sender = recipients[0]
    val messageId = MmsHelper.insert(
      IncomingMediaMessage(
        from = sender,
        sentTimeMillis = 2,
        serverTimeMillis = 2,
        receivedTimeMillis = 2,
        storyType = StoryType.STORY_WITH_REPLIES
      ),
      -1L
    ).get().messageId

    val messageBeforeMark = SignalDatabase.mms.getMessageRecord(messageId)
    assertFalse(messageBeforeMark.incomingStoryViewedAtTimestamp > 0)

    // WHEN
    SignalDatabase.mms.setIncomingMessageViewed(messageId)

    // THEN
    val messageAfterMark = SignalDatabase.mms.getMessageRecord(messageId)
    assertTrue(messageAfterMark.incomingStoryViewedAtTimestamp > 0)
  }

  @Test
  fun given5ViewedStories_whenIGetOrderedStoryRecipientsAndIds_thenIExpectLatestViewedFirst() {
    // GIVEN
    val messageIds = recipients.take(5).map {
      MmsHelper.insert(
        IncomingMediaMessage(
          from = it,
          sentTimeMillis = 2,
          serverTimeMillis = 2,
          receivedTimeMillis = 2,
          storyType = StoryType.STORY_WITH_REPLIES,
        ),
        -1L
      ).get().messageId
    }

    val randomizedOrderedIds = messageIds.shuffled()
    randomizedOrderedIds.forEach {
      SignalDatabase.mms.setIncomingMessageViewed(it)
      Thread.sleep(5)
    }

    // WHEN
    val result = SignalDatabase.mms.orderedStoryRecipientsAndIds
    val resultOrderedIds = result.map { it.messageId }

    // THEN
    assertEquals(randomizedOrderedIds.reversed(), resultOrderedIds)
  }

  @Test
  fun given15Stories_whenIGetOrderedStoryRecipientsAndIds_thenIExpectUnviewedThenInterspersedViewedAndSelfSendsAllDescending() {
    val myStoryThread = SignalDatabase.threads.getOrCreateThreadIdFor(myStory)

    val unviewedIds: List<Long> = (0 until 5).map {
      Thread.sleep(5)
      MmsHelper.insert(
        IncomingMediaMessage(
          from = recipients[it],
          sentTimeMillis = System.currentTimeMillis(),
          serverTimeMillis = 2,
          receivedTimeMillis = 2,
          storyType = StoryType.STORY_WITH_REPLIES,
        ),
        -1L
      ).get().messageId
    }

    val viewedIds: List<Long> = (0 until 5).map {
      Thread.sleep(5)
      MmsHelper.insert(
        IncomingMediaMessage(
          from = recipients[it],
          sentTimeMillis = System.currentTimeMillis(),
          serverTimeMillis = 2,
          receivedTimeMillis = 2,
          storyType = StoryType.STORY_WITH_REPLIES,
        ),
        -1L
      ).get().messageId
    }

    val interspersedIds: List<Long> = (0 until 10).map {
      Thread.sleep(5)
      if (it % 2 == 0) {
        SignalDatabase.mms.setIncomingMessageViewed(viewedIds[it / 2])
        viewedIds[it / 2]
      } else {
        MmsHelper.insert(
          recipient = myStory,
          sentTimeMillis = System.currentTimeMillis(),
          storyType = StoryType.STORY_WITH_REPLIES,
          threadId = myStoryThread
        )
      }
    }

    val result = SignalDatabase.mms.orderedStoryRecipientsAndIds
    val resultOrderedIds = result.map { it.messageId }

    assertEquals(unviewedIds.reversed() + interspersedIds.reversed(), resultOrderedIds)
  }
}
