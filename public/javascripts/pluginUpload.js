/*
 * ==================================================
 *  _____             _
 * |     |___ ___    |_|___
 * |  |  |  _| -_|_  | |_ -|
 * |_____|_| |___|_|_| |___|
 *                 |___|
 *
 * By Walker Crouse (windy) and contributors
 * (C) SpongePowered 2016-2017 MIT License
 * https://github.com/SpongePowered/Ore
 *
 * Validates and handles new plugin uploads.
 *
 * ==================================================
 */

var MAX_FILE_SIZE = 20971520;

/*
 * ==================================================
 * =                Helper functions                =
 * ==================================================
 */

function getAlert() {
    return $('.alert-file');
}

function clearIcon(e) {
    return e.removeClass('fa-spinner')
        .removeClass('fa-spin')
        .removeClass('fa-pencil')
        .removeClass('fa-upload');
}

function failure(message) {
    var alert = getAlert();
    var control = alert.find('.file-upload');

    var bs = alert.find('.alert');
    bs.removeClass('alert-info').addClass('alert-danger');
    var noticeIcon = bs.find('i');
    noticeIcon.removeClass('fa-file-archive-o').addClass('fa-exclamation-circle').tooltip({
        placement: 'left',
        title: message
    });

    // flash
    function flash(amount) {
        if (amount > 0) {
            noticeIcon.fadeOut('fast', function () {
                noticeIcon.fadeIn('fast', flash(amount - 1));
            });
        }
    }

    flash(7);
}

function failurePlugin(message) {
    failure(message);
    var alert = getAlert();
    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-success').addClass('btn-danger').prop('disabled', true);
    clearIcon(control.find('i')).addClass('fa-times');
}

function failureSig(message) {
    failure(message);
    var alert = getAlert();
    var control = alert.find('.file-upload');
    clearIcon(control.find('i')).addClass('fa-pencil');
}

function reset() {
    var alert = getAlert();
    alert.hide();

    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-danger').addClass('btn-success').prop('disabled', false);
    clearIcon(control.find('i')).addClass('fa-pencil');

    var bs = alert.find('.alert');
    bs.removeClass('alert-danger').addClass('alert-info');
    bs.find('i').removeClass('fa-exclamation-circle').addClass('fa-file-archive-o').tooltip('destroy');

    return alert;
}

function addSig(e) {
    e.preventDefault();
    $('#pluginSig').click();
}

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('#pluginFile').on('change', function() {
        var alert = reset();
        if (this.files.length == 0) {
            $('#form-upload')[0].reset();
            return;
        }
        var fileName = $(this).val().trim();
        var fileSize = this.files[0].size;
        if (!fileName) {
            alert.fadeOut(1000);
            return;
        }

        if (fileSize > MAX_FILE_SIZE) {
            failurePlugin('That file is too big. Plugins may be no larger than ' + filesize(MAX_FILE_SIZE) + '.');
        } else if (!fileName.endsWith('.zip') && !fileName.endsWith('.jar')) {
            failurePlugin('Only JAR and ZIP files are accepted.');
        }

        fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
        alert.find('.file-name').text(fileName);
        alert.find('.file-size').text(filesize(this.files[0].size));
        alert.fadeIn('slow');

        $('.btn-sign').removeClass('btn-success').addClass('btn-info').on('click', addSig);
    });

    $('#pluginSig').on('change', function() {
        console.log('sig changed');
        var fileName = $(this).val().trim();
        var alert = getAlert();
        var alertInner = alert.find('.alert');
        var button = alert.find('button');
        var icon = button.find('i');
        icon.removeClass('fa-pencil').addClass('fa-spinner fa-spin');
        setTimeout(function() {
            if (!fileName)
                return;
            alertInner.removeClass('alert-info alert-danger').addClass('alert-success');
            button.removeClass('btn-info').addClass('btn-success').off('click', addSig);

            icon.removeClass('fa-spinner fa-spin').addClass('fa-upload');

            var newTitle = 'Upload plugin';
            button.tooltip('hide')
                .attr('data-original-title', newTitle)
                .tooltip('fixTitle');
        }, 3000);
    });
});
