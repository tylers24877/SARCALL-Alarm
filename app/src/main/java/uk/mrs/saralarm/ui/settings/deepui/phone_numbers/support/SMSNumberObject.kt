package uk.mrs.saralarm.ui.settings.deepui.phone_numbers.support


class SMSNumberObject(ph: String, reply: Boolean) {
    var defaultReply: Boolean
    var phoneNumber: String
    init {
        phoneNumber = ph
        defaultReply = reply
    }
}