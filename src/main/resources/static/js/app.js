(function () {
    'use strict';

    var lineItemIndex = 0;

    function openModal(id) {
        var el = document.getElementById(id);
        if (el) {
            el.classList.add('open');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeModal(id) {
        var el = document.getElementById(id);
        if (el) {
            el.classList.remove('open');
            document.body.style.overflow = '';
        }
    }

    function closeAllModals() {
        document.querySelectorAll('.modal-overlay.open').forEach(function (m) {
            m.classList.remove('open');
        });
        document.body.style.overflow = '';
    }

    function showToast(message, type) {
        type = type || 'info';
        var container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container';
            document.body.appendChild(container);
        }
        var toast = document.createElement('div');
        toast.className = 'toast toast-' + type;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(function () {
            toast.style.opacity = '0';
            toast.style.transition = 'opacity 0.3s ease';
            setTimeout(function () { toast.remove(); }, 300);
        }, 3500);
    }

    function handleWardChange(selectEl) {
        var wardId = selectEl.value;
        var target = selectEl.getAttribute('data-target');
        var url = selectEl.getAttribute('data-url');
        if (!wardId || !target || !url) return;
        var finalUrl = url.replace('{wardId}', wardId);
        htmx.ajax('GET', finalUrl, { target: target, swap: 'innerHTML' });
    }

    function validateForm(formEl) {
        var valid = true;
        formEl.querySelectorAll('[required]').forEach(function (input) {
            if (!input.value.trim()) {
                input.classList.add('error');
                valid = false;
            } else {
                input.classList.remove('error');
            }
        });
        return valid;
    }

    function addLineItem() {
        lineItemIndex++;
        var container = document.getElementById('line-items');
        if (!container) return;
        var div = document.createElement('div');
        div.className = 'line-item grid-3 mb-2';
        div.innerHTML =
            '<div class="form-group">' +
                '<label class="form-label">Drug SKU</label>' +
                '<input type="text" class="form-input" name="lineItems[' + lineItemIndex + '].drugSkuId" placeholder="SKU ID" required>' +
            '</div>' +
            '<div class="form-group">' +
                '<label class="form-label">Quantity</label>' +
                '<input type="number" class="form-input" name="lineItems[' + lineItemIndex + '].requestedQuantity" min="1" required>' +
            '</div>' +
            '<div class="form-group">' +
                '<label class="form-label">Unit Price</label>' +
                '<input type="number" step="0.01" class="form-input" name="lineItems[' + lineItemIndex + '].unitPrice" placeholder="0.00">' +
            '</div>' +
            '<div class="form-group" style="grid-column:span 3">' +
                '<label class="form-label">Justification</label>' +
                '<input type="text" class="form-input" name="lineItems[' + lineItemIndex + '].justification" placeholder="Justification">' +
            '</div>';
        container.appendChild(div);
    }

    document.addEventListener('click', function (e) {
        if (e.target.classList.contains('modal-overlay')) {
            e.target.classList.remove('open');
            document.body.style.overflow = '';
        }
        if (e.target.matches('[data-add-line-item]')) {
            e.preventDefault();
            addLineItem();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeAllModals();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.modal-overlay').forEach(function (overlay) {
            overlay.querySelectorAll('.modal-close, [data-dismiss="modal"]').forEach(function (btn) {
                btn.addEventListener('click', function () {
                    closeModal(overlay.id);
                });
            });
        });

        document.querySelectorAll('[data-toggle="modal"]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var target = btn.getAttribute('data-target');
                if (target) openModal(target);
            });
        });

        document.querySelectorAll('.ward-select:not([x-data] *)').forEach(function (sel) {
            sel.addEventListener('change', function () {
                handleWardChange(this);
            });
        });
    });

    function handleLogout(e) {
        if (e) e.preventDefault();
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('tokenType');
        htmx.config.defaultHeaders = {};
        window.location.href = '/login';
    }

    window.MediChain = {
        openModal: openModal,
        closeModal: closeModal,
        closeAllModals: closeAllModals,
        showToast: showToast,
        validateForm: validateForm,
        handleWardChange: handleWardChange,
        addLineItem: addLineItem,
        handleLogout: handleLogout
    };

})();
