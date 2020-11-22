package ng.com.stemcoders.campusspace.net.models;

public class ModeratorModel extends BaseModel
{
    private int faculty_id;
    private int department_id;
    private int level_id;

    private String username;
    private String email;
    private String full_name;
    private String gender;
    private String phone;
    private String reg_date;
    private String faculty;
    private String department;
    private String level;

    public int getFaculty_id()
    {
        return faculty_id;
    }

    public void setFaculty_id(int faculty_id)
    {
        this.faculty_id = faculty_id;
    }

    public int getDepartment_id()
    {
        return department_id;
    }

    public void setDepartment_id(int department_id)
    {
        this.department_id = department_id;
    }

    public int getLevel_id()
    {
        return level_id;
    }

    public void setLevel_id(int level_id)
    {
        this.level_id = level_id;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getFull_name()
    {
        return full_name;
    }

    public void setFull_name(String full_name)
    {
        this.full_name = full_name;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getReg_date()
    {
        return reg_date;
    }

    public void setReg_date(String reg_date)
    {
        this.reg_date = reg_date;
    }

    public String getFaculty()
    {
        return faculty;
    }

    public void setFaculty(String faculty)
    {
        this.faculty = faculty;
    }

    public String getDepartment()
    {
        return department;
    }

    public void setDepartment(String department)
    {
        this.department = department;
    }

    public String getLevel()
    {
        return level;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }
}
