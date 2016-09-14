function markRead(notification) {
    var btn = notification.find('.btn-mark-read');
    btn.removeClass('btn-mark-read fa-check').addClass('fa-spinner fa-spin');
    $.ajax({
        url: '/notifications/read/' + notification.data('id'),
        complete: function() {
            btn.removeClass('fa-spinner fa-spin').addClass('btn-mark-read fa-check');
        },
        success: function() {
            notification.fadeOut('slow', function() {
                item.remove();
            })
        }
    })
}

$(function() {

    var invites = $('.invite-content');
    invites.css('height', invites.width());

    $('.btn-mark-all-read').click(function() {
       $('.btn-mark-read').click();
    });

    $('.btn-mark-read').click(function() {
        markRead($(this).closest('.notification'));
    });

    $('.notification').click(function(e) {
        if (e.target !== this)
            return;

        var action = $(this).data('action');
        $(this).find('.btn-mark-read').click();
        if (action !== 'none')
            window.location.href = action;
    });

    $('.select-notifications').on('change', function() {
        window.location = '/notifications/?notification_filter=' + $(this).val();
    });

    $('.select-invites').on('change', function() {
        window.location = '/notifications/?invite_filter=' + $(this).val();
    });

    $('.btn-invite').click(function() {
        var btn = $(this);
        var invite = btn.closest('.invite-content');
        var choice = invite.find('.invite-choice');
        choice.animate({
            right: "+=200"
        }, 200, function() {
            choice.hide().css('right', 'auto');
            invite.find('.invite-dismiss').fadeIn('slow');
            var clazz = btn.hasClass('btn-accept') ? '.invite-accepted' : '.invite-declined';
            invite.find(clazz).fadeIn('slow');
        });
    });

    $('.btn-undo').click(function() {
        var invite = $(this).closest('.invite-content');
        var accepted = invite.find('.invite-accepted');
        invite.find('.invite-dismiss').fadeOut('slow');
        accepted.fadeOut('slow', function() {
            var choice = invite.find('.invite-choice');
            choice.css('right', '+=200').show().animate({
                right: "5%"
            }, 200)
        });
    });

    $('.invite-dismiss').click(function() {
        var invite = $(this).closest('.invite-content');
        invite.fadeOut('slow', function() {
            invite.remove();
        })
    });

});
