var DEFAULT_COLOR = null;

function initChannelNew(color) {
    initChannelManager(
        '#channel-new', '', color, 'New channel', null, null,
        'Create channel'
    );
}

function getForm() {
    return $('#form-publish');
}

function getSelect() {
    return $('#select-channel');
}

function setColorInput(val) {
    getForm().find('.channel-color-input').val(val);
}

$(function() {
    initChannelNew(DEFAULT_COLOR);

    getSelect().change(function() {
        var selected = $(this).find(':selected');
        setColorInput(selected.data('color'));
        // Update form action
        var form = getForm();
        var action = form.attr('action');
        var version = action.substr(action.lastIndexOf('/') + 1, action.length);
        var actionBase = action.substr(0, action.lastIndexOf('/'));
        actionBase = actionBase.substr(0, actionBase.lastIndexOf('/'));
        var newAction = actionBase + '/' + selected.val() + '/' + version;
        form.attr('action', newAction);
    });

    onCustomSubmit = function(toggle, channelName, channelHex, title, submit) {
        // Add new name to select
        var select = getSelect();
        var exists = select.find('option').find(function() {
            return $(this).val().toLowerCase() === channelName.toLowerCase();
        }).length !== 0;

        if (!exists) {
            setColorInput(channelHex);
            select.find(':selected').removeAttr('selected');
            select.append('<option data-color="' + channelHex + '" '
                                + 'value="' + channelName + '" '
                                + 'selected>' + channelName + '</option>');
        }

        $('#channel-manage').modal('hide');
        initChannelNew(DEFAULT_COLOR);
    }
});
