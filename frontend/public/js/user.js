// 마이페이지 기능
// 필요 로직
// 1. 사용자 프로필 가져오는 메서드 (fetch get 요청)
// 2. 닉네임 중복 검사 메서드(fetch get 요청)
// 3. 프로필 이미지 presignedURl get 메서드 (fetch post 요청)
// 4. 최종 수정완료 메서드 (fetch post 요청)
// 5. presignedUrl로 프로필 이미지 put 요청 메서드 (fetch put 요청)
// 6. 최종 회원탈퇴 요청 메서드 

document.addEventListener('DOMContentLoaded', () => {

    // -----------------------------------------------------------------------------
    // ## DOM 요소 및 상태 변수 설정
    // -----------------------------------------------------------------------------
    const profileImagePreview = document.getElementById('profile-image-preview');
    const profileImageUpload = document.getElementById('profile-image-upload');
    const userEmailInput = document.getElementById('user-email');
    const userNicknameInput = document.getElementById('user-nickname');
    const checkNicknameButton = document.getElementById('check-nickname-button');
    const updateProfileButton = document.getElementById('update-profile-button');
    const withdrawButton = document.getElementById('withdraw-button');
    const imageValidation = document.getElementById('image-validation');

    // 모달 관련 DOM 요소
    const withdrawModal = new bootstrap.Modal(document.getElementById('withdraw-modal'));
    const withdrawStep1 = document.getElementById('withdraw-step-1');
    const withdrawStep2 = document.getElementById('withdraw-step-2');
    const withdrawFooter1 = document.getElementById('withdraw-footer-1');
    const withdrawFooter2 = document.getElementById('withdraw-footer-2');
    const confirmWithdrawStep1Btn = document.getElementById('confirm-withdraw-step1');
    const confirmWithdrawFinalBtn = document.getElementById('confirm-withdraw-final');
    const withdrawPasswordInput = document.getElementById('withdraw-password');


    // 상태 관리 변수
    let originalNickname = '';
    let isNicknameChecked = false;
    let newProfileImageData = null;

    // -----------------------------------------------------------------------------
    // ## 핵심 기능 함수 정의
    // -----------------------------------------------------------------------------

    // 1. 사용자 프로필 가져오는 메서드
    const loadUserData = async () => {
        try {
            const response = await customFetch('http://localhost:8080/v1/users/me', { method: 'GET', credentials: 'include' });
            if (!response.ok) throw new Error('사용자 정보 로딩 실패');
            
            const ApiResponse = await response.json();
            const userData = ApiResponse.data;
            userNicknameInput.value = userData.nickname;
            userEmailInput.value = userData.email;
            profileImagePreview.src = userData.profileUrl || '/img/default-profile.png';
            originalNickname = userData.nickname;
        } catch (error) {
            console.error('사용자 정보 로딩 중 오류:', error);
            alert('사용자 정보를 불러오는데 실패했습니다. 다시 로그인해주세요.');
            window.location.href = '/v1/auth/login';
        }
    };
    
    // 2. 닉네임 중복 검사 메서드 
    const handleNicknameCheck = async () => {
        const newNickname = userNicknameInput.value.trim();

        if (newNickname === originalNickname) {
            alert('현재 닉네임과 동일합니다.');
            isNicknameChecked = false;
            return;
        }
        if (newNickname.length < 2 || newNickname.length > 10) {
            alert('닉네임은 2자 이상 10자 이하로 입력해주세요.');
            return;
        }

        try {
            // 쿼리 파라미터로 닉네임을 전송
            const apiUrl = `http://localhost:8080/v1/users/me/nickname?nickname=${encodeURIComponent(newNickname)}`;
            
            const response = await customFetch(apiUrl, {
                method: 'GET',
                credentials: 'include'
                // GET 요청이므로 body와 Content-Type 헤더는 제거
            });
            
            if (!response.ok) throw new Error('중복 확인 요청 실패');

            const ApiResponse = await response.json();
            const isAvailable = ApiResponse.data;
            if (isAvailable) {
                alert('사용 가능한 닉네임입니다.');
                isNicknameChecked = true;
            } else {
                alert('이미 사용 중인 닉네임입니다.');
                isNicknameChecked = false;
            }
        } catch (error) {
            console.error('닉네임 중복 확인 중 오류:', error);
            alert('중복 확인 중 오류가 발생했습니다.');
            isNicknameChecked = false;
        }
    };
    
    // 오류 메시지 표시 함수
    const showError = (message, isError = true) => {
        imageValidation.textContent = message;
        imageValidation.style.display = 'block';
        imageValidation.style.color = isError ? 'red' : 'blue';
    };

    // 메시지 숨기기 함수
    const hideMessage = () => {
        imageValidation.style.display = 'none';
    };

    // 3. 프로필 이미지 presignedURL get 메서드 
    const handleImageSelect = async (event) => {
        const file = event.target.files[0];
        if (!file) return;

        // 파일 형식 검사
        const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            showError('JPG, PNG, WEBP 형식의 이미지만 업로드 가능합니다.');
            event.target.value = '';
            return;
        }

        try {
            showError('이미지 업로드 중...', false);

            const presignedResponse = await customFetch('http://localhost:8080/v1/users/presignedUrl', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ fileName: file.name })
            });
            if (!presignedResponse.ok) throw new Error('Presigned URL 요청 실패');
            const ApiResponse = await presignedResponse.json();
            const { s3_key, preSignedUrl } = ApiResponse.data;
            newProfileImageData = { file, s3_key, preSignedUrl };
            profileImagePreview.src = URL.createObjectURL(file);
            hideMessage();
        } catch (error) {
            console.error('이미지 처리 중 오류:', error);
            showError('이미지 처리 중 오류가 발생했습니다.');
            newProfileImageData = null;
            event.target.value = '';
        }
    };

    // 4. 최종 수정완료 메서드
    const handleProfileUpdate = async () => {
        updateProfileButton.disabled = true;
        updateProfileButton.textContent = '저장 중...';
        try {
            const updateTasks = [];
            const newNickname = userNicknameInput.value.trim();
            if (newNickname !== originalNickname) {
                if (!isNicknameChecked) throw new Error('닉네임 중복 확인을 완료해주세요.');
                updateTasks.push(
                    customFetch('http://localhost:8080/v1/users/me/nickname', {
                        method: 'PUT',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                        body: JSON.stringify({ nickname: newNickname })
                    })
                );
            }
            if (newProfileImageData) {
                await uploadImageToS3(newProfileImageData.preSignedUrl, newProfileImageData.file);
                updateTasks.push(
                    customFetch('http://localhost:8080/v1/users/me/image', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                        body: JSON.stringify({ s3_key: newProfileImageData.s3_key })
                    })
                );
            }
            const responses = await Promise.all(updateTasks);
            responses.forEach(res => {
                if (!res.ok) throw new Error('프로필 업데이트 중 일부가 실패했습니다.');
            });
            alert('프로필이 성공적으로 수정되었습니다.');
            window.location.reload();
        } catch (error) {
            console.error('프로필 수정 중 오류:', error);
            alert(error.message || '프로필 수정 중 오류가 발생했습니다.');
        } finally {
            updateProfileButton.disabled = false;
            updateProfileButton.textContent = '수정완료';
        }
    };

    // 5. presignedUrl로 프로필 이미지 put 요청 메서드 
    const uploadImageToS3 = async (url, file) => {
        showError('프로필 이미지 저장 중...', false);
        const response = await fetch(url, {
            method: 'PUT',
            body: file,
            headers: { 'Content-Type': file.type }
        });
        if (!response.ok) throw new Error('S3 이미지 업로드 실패');
    };

    // 6. 최종 회원탈퇴 요청 메서드 
    const handleWithdrawal = async () => {
        const password = withdrawPasswordInput.value;
        if (!password) {
            alert('비밀번호를 입력해주세요.');
            return;
        }

        try {
            const response = await customFetch('http://localhost:8080/v1/users/me', {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ password })
            });

            if (response.status === 200) { // No Content
                alert('회원탈퇴가 완료되었습니다. 이용해주셔서 감사합니다.');
                document.cookie = 'refresh=; Max-Age=0; path=/;';
                localStorage.removeItem('accessToken');
                
                window.location.href = '/v1/auth/login';
            } else {
                // 비밀번호가 틀렸거나 다른 에러 발생
                const errorData = await response.json();
                throw new Error(errorData.message || '회원탈퇴에 실패했습니다.');
            }
        } catch (error) {
            console.error('회원탈퇴 처리 중 오류:', error);
            alert(error.message);
        }
    };


    // -----------------------------------------------------------------------------
    // ## 이벤트 리스너 연결
    // -----------------------------------------------------------------------------

    userNicknameInput.addEventListener('input', () => {
        if (userNicknameInput.value.trim() !== originalNickname) isNicknameChecked = false;
    });

    checkNicknameButton.addEventListener('click', handleNicknameCheck);
    profileImageUpload.addEventListener('change', handleImageSelect);
    updateProfileButton.addEventListener('click', handleProfileUpdate);
    
    // 모달의 '확인' 버튼 클릭 시, 2단계(비밀번호 입력)로 전환
    confirmWithdrawStep1Btn.addEventListener('click', () => {
        withdrawStep1.style.display = 'none';
        withdrawFooter1.style.display = 'none';
        withdrawStep2.style.display = 'block';
        withdrawFooter2.style.display = 'block';
        withdrawPasswordInput.focus(); // 비밀번호 입력창에 자동 포커스
    });

    // 모달의 '최종 탈퇴' 버튼 클릭 시, 탈퇴 함수 호출
    confirmWithdrawFinalBtn.addEventListener('click', handleWithdrawal);
    
    // 모달이 닫힐 때, 다시 1단계로 리셋
    document.getElementById('withdraw-modal').addEventListener('hidden.bs.modal', () => {
        withdrawPasswordInput.value = '';
        withdrawStep2.style.display = 'none';
        withdrawFooter2.style.display = 'none';
        withdrawStep1.style.display = 'block';
        withdrawFooter1.style.display = 'block';
    });

    // 페이지가 처음 로드될 때 사용자 정보를 불러옵니다.
    loadUserData();
});
