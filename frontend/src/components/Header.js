import React, { useState, useEffect } from 'react'
import { useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import logo from '../assets/logo.svg'
import search from '../assets/search.svg'
import ProfileFrame from './profileFrame'
import '../styles/header.css'
import SearchIcon from '@mui/icons-material/Search'
import Box from '@mui/material/Box'
import Modal from '@mui/material/Modal'
import LogIn from '../pages/sign_folder/LogIn/LogIn'
// import Button from '@mui/material/Button'

const wholeTextArray = [
  'apple',
  'applemango',
  'banana',
  'coding',
  'candy',
  'camera',
  'javascript',
  'TENTEN',
  '텐텐',
  '터쿠아즈',
  '마젠타',
  '애프리콧',
  '세이지',
  '플라밍고',
  '라피스'
]
function Header(props) {
  // const [login, setLogin] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const [isHaveInputValue, setIsHaveInputValue] = useState(false)
  const [dropDownList, setDropDownList] = useState(wholeTextArray)
  const [dropDownItemIndex, setDropDownItemIndex] = useState(-1)
  const [open, setOpen] = useState(false)
  const handleOpen = () => setOpen(true)
  const handleClose = () => setOpen(false)
  const userLogin = useSelector((state) => {
    return state.user.isLogged
  })

  const IsLogin = () => {
    // console.log(login)
    if (userLogin) {
      return (
        <>
          <div className="upload-profile">
            <button className="upload-btn" onClick={goRecordRoom}>
              + 업로드
            </button>
            <ProfileFrame />
          </div>
        </>
      )
    } else {
      return (
        <>
          {/* onclick 시 navigateToLogin 함수 주석해제, 실행 */}
          <button className="login-btn" onClick={handleOpen}>
            로그인
          </button>
        </>
      )
    }
  }

  // const loginmodalOpen = () => {
  //   setLogin(true)
  //   props.openModal(true)
  // }

  const goRecordRoom = () => {
    navigate('/RecordRoom')
  }

  const showDropDownList = () => {
    if (inputValue === '') {
      setIsHaveInputValue(false)
      setDropDownList([])
    } else {
      const choosenTextList = wholeTextArray.filter((textItem) =>
        textItem.toLowerCase().startsWith(inputValue.toLowerCase())
      )
      if (Array.isArray(choosenTextList) && choosenTextList.length === 0) {
        setIsHaveInputValue(false)
        setDropDownList([])
      } else {
        setIsHaveInputValue(true)
        setDropDownList(choosenTextList)
      }
    }
  }

  const changeInputValue = (event) => {
    setInputValue(event.target.value)
    // setIsHaveInputValue(true)
  }

  const clickDropDownItem = (clickedItem) => {
    setInputValue(clickedItem)
    setIsHaveInputValue(false)
  }

  const handleDropDownKey = (event) => {
    // input에 값이 있을때만 작동
    if (isHaveInputValue) {
      if (
        event.key === 'ArrowDown' &&
        dropDownList.length - 1 > dropDownItemIndex
      ) {
        setDropDownItemIndex(dropDownItemIndex + 1)
      }

      if (event.key === 'ArrowUp' && dropDownItemIndex >= 0) {
        setDropDownItemIndex(dropDownItemIndex - 1)
      }
      if (event.key === 'Enter' && dropDownItemIndex >= 0) {
        clickDropDownItem(dropDownList[dropDownItemIndex])
        setDropDownItemIndex(-1)
      }
    }
  }

  useEffect(showDropDownList, [inputValue], [isHaveInputValue])
  const navigate = useNavigate()
  // const navigateToSearchView = () => {
  //   navigate('/SearchView')
  // }
  const navigateToMain = () => {
    navigate('/')
  }
  // const navigateToLogin = () => {
  //   setLogin(true)
  //   navigate('/Login')
  // }

  return (
    <header className="header">
      <Modal open={open} onClose={handleClose}>
        <Box>
          <LogIn handleClose={handleClose} />
        </Box>
      </Modal>
      <img className="logo-img" src={logo} onClick={navigateToMain}></img>
      <div className="header-container">
        {/* 검색창 */}
        {/* <InputBox isHaveInputValue={isHaveInputValue}>
            <Input
              type="text"
              value={inputValue}
              onChange={changeInputValue}
              onKeyUp={handleDropDownKey}
            />
            <DeleteButton onClick={() => setInputValue('')}>
              &times;
            </DeleteButton>
          </InputBox>
          {isHaveInputValue && (
            <DropDownBox>
              {dropDownList.length === 0 && (
                <DropDownItem>해당하는 단어가 없습니다</DropDownItem>
              )}
              {dropDownList.map((dropDownItem, dropDownIndex) => {
                return (
                  <DropDownItem
                    key={dropDownIndex}
                    onClick={() => clickDropDownItem(dropDownItem)}
                    onMouseOver={() => setDropDownItemIndex(dropDownIndex)}
                    className={
                      dropDownItemIndex === dropDownIndex ? 'selected' : ''
                    }
                  >
                    {dropDownItem}
                  </DropDownItem>
                )
              })}
            </DropDownBox>
          )} */}
        <div className="header-search">
          <input
            className="search-input"
            placeholder="검색어를 입력해주세요."
            type="text"
            value={inputValue}
            onChange={changeInputValue}
            onKeyUp={handleDropDownKey}
          />
          <img
            onClick={() => setInputValue('')}
            className="search-icon"
            src={search}
          />
        </div>
        {isHaveInputValue && (
          <ul className="dropdownbox">
            {dropDownList.map((dropDownItem, dropDownIndex) => {
              return (
                <li
                  key={dropDownIndex}
                  onClick={() => clickDropDownItem(dropDownItem)}
                  onMouseOver={() => setDropDownItemIndex(dropDownIndex)}
                  className={
                    dropDownItemIndex === dropDownIndex
                      ? 'dropDownItemIndex selected'
                      : 'dropDownItemIndex'
                  }
                >
                  <SearchIcon
                    style={{
                      width: '16px',
                      height: '16px',
                      marginRight: '5px'
                    }}
                  />
                  {dropDownItem}
                </li>
              )
            })}
          </ul>
        )}
      </div>
      {/* 로그인버튼 or 업로드버튼 + 프로필사진 */}
      <IsLogin />
    </header>
  )
}

// const activeBorderRadius = '16px 16px 0 0'
// const inactiveBorderRadius = '16px 16px 16px 16px'
// const InputBox = styled.div`
//   display: flex;
//   flex-direction: row;
//   padding: 16px;
//   border: 1px solid rgba(0, 0, 0, 0.3);
//   border-radius: ${(props) =>
//     props.isHaveInputValue ? activeBorderRadius : inactiveBorderRadius};
//   z-index: 3;

//   &:focus-within {
//     box-shadow: 0 10px 10px rgb(0, 0, 0, 0.3);
//   }
// `

// const Input = styled.input`
//   flex: 1 0 0;
//   margin: 0;
//   padding: 0;
//   background-color: transparent;
//   border: none;
//   outline: none;
//   font-size: 16px;
// `

// const DeleteButton = styled.div`
//   cursor: pointer;
// `

// const DropDownBox = styled.ul`
//   display: block;
//   margin: 0 auto;
//   padding: 8px 0;
//   background-color: white;
//   border: 1px solid rgba(0, 0, 0, 0.3);
//   border-top: none;
//   border-radius: 0 0 16px 16px;
//   box-shadow: 0 10px 10px rgb(0, 0, 0, 0.3);
//   list-style-type: none;
//   z-index: 3;
// `

// const DropDownItem = styled.li`
//   padding: 0 16px;

//   &.selected {
//     background-color: lightgray;
//   }
// `

export default Header
