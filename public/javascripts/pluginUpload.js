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

function failure(message) {
    var alert = getAlert();
    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-success').addClass('btn-danger').prop('disabled', true);

    var bs = alert.find('.alert');
    bs.removeClass('alert-info').addClass('alert-danger');
    bs.find('i').removeClass('fa-file-archive-o').addClass('fa-exclamation-circle').tooltip({
        placement: 'left',
        title: message
    });
}

function reset() {
    var alert = getAlert();
    alert.hide();

    var control = alert.find('.file-upload');
    control.find('button').removeClass('btn-danger').addClass('btn-success').prop('disabled', false);

    var bs = alert.find('.alert');
    bs.removeClass('alert-danger').addClass('alert-info');
    bs.find('i').removeClass('fa-exclamation-circle').addClass('fa-file-archive-o').tooltip('destroy');

    return alert;
}

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('#pluginFile').on('change', function() {
        var alert = reset();
        var fileName = $(this).val().trim();
        var fileSize = this.files[0].size;
        if (!fileName) {
            alert.fadeOut(1000);
            return;
        }

        if (fileSize > MAX_FILE_SIZE) {
            failure('That file is too big. Plugins may be no larger than ' + filesize(MAX_FILE_SIZE) + '.');
        } else if (!fileName.endsWith('.zip') && !fileName.endsWith('.jar')) {
            failure('Only JAR and ZIP files are accepted.');
        }

        fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
        alert.find('.file-name').text(fileName);
        alert.find('.file-size').text(filesize(this.files[0].size));
        alert.fadeIn('slow');
    });

    // $('.file-upload').find('button').click(function() {
    //     $(this).find('i').removeClass('fa-upload').addClass('fa-spinner fa-spin');
    // });

    $('.btn-sign').click(function(e) {
        e.preventDefault();
        $('#pluginSig').click();
    });

    $('#pluginSig').on('change', function() {
        var fileName = $(this).val().trim();
        var alert = getAlert();
        var alertInner = alert.find('.alert');
        var button = alert.find('button');
        var icon = button.find('i');
        icon.removeClass('fa-pencil').addClass('fa-spinner fa-spin');
        setTimeout(function() {
            if (!fileName)
                return;
            if (!fileName.endsWith('.sig')) {
                failure('Please sign your plugin with a .sig detached PGP signature.');
                return;
            }
            alertInner.removeClass('alert-info').addClass('alert-success');
            button.removeClass('btn-info btn-sign').addClass('btn-success');
            icon.removeClass('fa-spinner fa-spin').addClass('fa-upload').prop('title', 'Upload plugin');
        }, 3000);
    });
});
